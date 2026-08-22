package com.reward.platform.api.controller

import com.reward.platform.api.dto.ProgramResponse
import com.reward.platform.api.dto.TenantProvisionRequest
import com.reward.platform.api.dto.TenantProvisionResponse
import com.reward.platform.api.dto.TenantResponse
import com.reward.platform.api.dto.TierSetup
import com.reward.platform.api.dto.TierResponse
import com.reward.platform.api.entity.ProgramEntity
import com.reward.platform.api.entity.TenantEntity
import com.reward.platform.api.entity.TierEntity
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.TenantRepository
import com.reward.platform.api.repository.TierRepository
import com.reward.platform.api.security.ApiKeyService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.beans.factory.annotation.Value
import java.util.UUID

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/provisioning")
class ProvisioningController(
    private val tenantRepository: TenantRepository,
    private val programRepository: ProgramRepository,
    private val tierRepository: TierRepository,
    @Value("\${PLATFORM_BASE_DOMAIN:indie-state.local}") private val baseDomain: String
) {

    @PostMapping("/tenants")
    @Transactional
    fun provisionTenant(
        @Valid @RequestBody request: TenantProvisionRequest
    ): ResponseEntity<TenantProvisionResponse> {
        require(request.slug.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*"))) {
            "Slug must contain lowercase letters, numbers, and hyphens only"
        }
        require(tenantRepository.findBySlug(request.slug) == null) { "Tenant slug already exists" }

        val tenantId = UUID.randomUUID().toString()
        val apiKey = ApiKeyService.generate()
        val tenant = tenantRepository.save(
            TenantEntity(
                id = tenantId,
                name = request.name,
                slug = request.slug,
                baseUrl = "https://${request.slug}.$baseDomain",
                schemaName = request.slug.replace('-', '_'),
                apiKeyHash = ApiKeyService.hash(apiKey),
                adminEmail = request.adminEmail
            )
        )

        val program = programRepository.save(
            ProgramEntity(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                name = request.programName,
                currency = request.currency ?: "INR",
                timezone = request.timezone ?: "Asia/Kolkata",
                status = "ACTIVE",
                earningRate = request.earningRate,
                redemptionRate = request.redemptionRate
            )
        )

        val tierSetup = request.tiers ?: listOf(
            TierSetup("SILVER", 1, 0),
            TierSetup("GOLD", 2, 1000),
            TierSetup("PLATINUM", 3, 5000)
        )
        val tiers = tierRepository.saveAll(
            tierSetup.sortedBy { it.rank }.map {
                TierEntity(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    programId = program.id,
                    name = it.name.uppercase(),
                    rank = it.rank,
                    thresholdPoints = it.thresholdPoints,
                    multiplier = it.multiplier
                )
            }
        )

        return ResponseEntity.ok(
            TenantProvisionResponse(
                tenant = TenantResponse.from(tenant),
                program = ProgramResponse.from(program),
                tiers = tiers.map {
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
                apiKey = apiKey
            )
        )
    }
}
