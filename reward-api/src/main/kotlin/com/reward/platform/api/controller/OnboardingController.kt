package com.reward.platform.api.controller

import com.reward.platform.api.dto.EnterpriseInquiryRequest
import com.reward.platform.api.dto.EnterpriseInquiryResponse
import com.reward.platform.api.dto.EnterpriseProvisionRequest
import com.reward.platform.api.dto.EnterpriseProvisionResponse
import com.reward.platform.api.dto.ProgramResponse
import com.reward.platform.api.dto.SelfServeRegisterRequest
import com.reward.platform.api.dto.SelfServeRegisterResponse
import com.reward.platform.api.dto.SponsorResponse
import com.reward.platform.api.dto.SystemUserBootstrapCredentials
import com.reward.platform.api.dto.SystemUserProfileResponse
import com.reward.platform.api.dto.TenantProvisionRequest
import com.reward.platform.api.dto.TenantProvisionResponse
import com.reward.platform.api.dto.TenantResponse
import com.reward.platform.api.dto.TierResponse
import com.reward.platform.api.entity.OnboardingRequestEntity
import com.reward.platform.api.repository.OnboardingRequestRepository
import com.reward.platform.api.security.JwtService
import com.reward.platform.api.service.TenantProvisioningService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api")
class OnboardingController(
    private val tenantProvisioningService: TenantProvisioningService,
    private val onboardingRequestRepository: OnboardingRequestRepository,
    private val jwtService: JwtService,
    @Value("\${ops.provisioning.key:dev-ops-key}") private val opsProvisioningKey: String
) {

    @PostMapping("/public/register")
    @Transactional
    fun registerSelfServe(
        @Valid @RequestBody request: SelfServeRegisterRequest
    ): ResponseEntity<SelfServeRegisterResponse> {
        val provisioned = tenantProvisioningService.provisionTenant(
            request = TenantProvisionRequest(
                name = request.businessName,
                slug = request.slug,
                adminEmail = request.adminEmail,
                programName = request.programName,
                currency = request.currency,
                timezone = request.timezone,
                earningRate = request.earningRate,
                redemptionRate = request.redemptionRate
            ),
            adminPasswordOverride = request.adminPassword
        )

        onboardingRequestRepository.save(
            OnboardingRequestEntity(
                companyName = request.businessName,
                contactName = request.adminEmail.substringBefore('@').ifBlank { request.businessName },
                contactEmail = request.adminEmail,
                requestedPlan = "SELF_SERVE",
                customPricingRequired = false,
                status = "PROVISIONED",
                tenantId = provisioned.tenant.id,
                updatedAt = Instant.now()
            )
        )

        val (accessToken, expiresInSeconds) = jwtService.createAccessToken(provisioned.systemUser)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            SelfServeRegisterResponse(
                accessToken = accessToken,
                expiresInSeconds = expiresInSeconds,
                user = SystemUserProfileResponse.from(provisioned.systemUser),
                tenant = TenantResponse.from(provisioned.tenant),
                program = ProgramResponse.from(provisioned.program),
                hostSponsor = SponsorResponse.from(provisioned.hostSponsor)
            )
        )
    }

    @PostMapping("/public/enterprise-inquiries")
    fun createEnterpriseInquiry(
        @Valid @RequestBody request: EnterpriseInquiryRequest
    ): ResponseEntity<EnterpriseInquiryResponse> {
        val entity = onboardingRequestRepository.save(
            OnboardingRequestEntity(
                companyName = request.companyName,
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                requestedPlan = "ENTERPRISE",
                companySize = request.companySize,
                expectedMonthlyMembers = request.expectedMonthlyMembers,
                expectedMonthlyTransactions = request.expectedMonthlyTransactions,
                notes = request.notes,
                customPricingRequired = true,
                status = "NEW"
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(
            EnterpriseInquiryResponse(
                onboardingRequestId = entity.id,
                status = entity.status,
                message = "Enterprise onboarding request submitted. Our team will contact you for custom pricing."
            )
        )
    }

    @PostMapping("/admin/provisioning/enterprise")
    @Transactional
    fun provisionEnterpriseTenant(
        @Valid @RequestBody request: EnterpriseProvisionRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<EnterpriseProvisionResponse> {
        val providedOpsKey = httpRequest.getHeader("X-Ops-Key")
        require(!providedOpsKey.isNullOrBlank() && providedOpsKey == opsProvisioningKey) {
            "Valid X-Ops-Key header is required"
        }

        val provisioned = tenantProvisioningService.provisionTenant(request.tenant)

        val onboardingRequest = request.onboardingRequestId?.let { id ->
            onboardingRequestRepository.findById(id).orElse(null)?.let { existing ->
                onboardingRequestRepository.save(
                    existing.copy(
                        status = "PROVISIONED",
                        tenantId = provisioned.tenant.id,
                        updatedAt = Instant.now()
                    )
                )
            }
        }

        val response = TenantProvisionResponse(
            tenant = TenantResponse.from(provisioned.tenant),
            program = ProgramResponse.from(provisioned.program),
            hostSponsor = SponsorResponse.from(provisioned.hostSponsor),
            tiers = provisioned.tiers.map {
                TierResponse(
                    id = it.id,
                    tenantId = it.tenantId,
                    programId = it.programId,
                    name = it.name,
                    rank = it.rank,
                    thresholdPoints = it.thresholdPoints,
                    multiplier = it.multiplier,
                    createdAt = it.createdAt
                )
            },
            apiKey = provisioned.apiKey,
            systemUser = SystemUserBootstrapCredentials(
                email = provisioned.systemUser.email,
                username = provisioned.systemUser.username,
                temporaryPassword = provisioned.rawPassword,
                role = provisioned.systemUser.role
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(
            EnterpriseProvisionResponse(
                onboardingRequestId = onboardingRequest?.id,
                customPricingModel = request.customPricingModel,
                contractReference = request.contractReference,
                provisioned = response
            )
        )
    }
}