package com.reward.platform.api.service

import com.reward.platform.api.dto.TierSetup
import com.reward.platform.api.dto.TenantProvisionRequest
import com.reward.platform.api.entity.BranchEntity
import com.reward.platform.api.entity.BranchRuleEntity
import com.reward.platform.api.entity.ProgramEntity
import com.reward.platform.api.entity.SponsorEntity
import com.reward.platform.api.entity.SponsorLocationEntity
import com.reward.platform.api.entity.SystemUserEntity
import com.reward.platform.api.entity.TenantEntity
import com.reward.platform.api.entity.TierEntity
import com.reward.platform.api.repository.BranchRepository
import com.reward.platform.api.repository.BranchRuleRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SystemUserRepository
import com.reward.platform.api.repository.TenantRepository
import com.reward.platform.api.repository.TierRepository
import com.reward.platform.api.security.ApiKeyService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ProvisionedTenantResult(
    val tenant: TenantEntity,
    val program: ProgramEntity,
    val hostSponsor: SponsorEntity,
    val tiers: List<TierEntity>,
    val apiKey: String,
    val systemUser: SystemUserEntity,
    val rawPassword: String
)

@Service
class TenantProvisioningService(
    private val tenantRepository: TenantRepository,
    private val programRepository: ProgramRepository,
    private val tierRepository: TierRepository,
    private val branchRepository: BranchRepository,
    private val branchRuleRepository: BranchRuleRepository,
    private val sponsorRepository: SponsorRepository,
    private val sponsorLocationRepository: SponsorLocationRepository,
    private val systemUserRepository: SystemUserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${PLATFORM_BASE_DOMAIN:benevo.io}") private val baseDomain: String
) {
    companion object {
        const val DEFAULT_BRANCH_CODE = "DEFAULT_MAIN"
        const val DEFAULT_LOCATION_CODE = "ONLINE_DEFAULT"
    }

    @Transactional
    fun provisionTenant(
        request: TenantProvisionRequest,
        adminPasswordOverride: String? = null,
        role: String = "TENANT_ADMIN"
    ): ProvisionedTenantResult {
        require(request.slug.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*"))) {
            "Slug must contain lowercase letters, numbers, and hyphens only"
        }
        require(tenantRepository.findBySlug(request.slug) == null) { "Tenant slug already exists" }
        require(systemUserRepository.findByEmailIgnoreCase(request.adminEmail) == null) {
            "Admin email already registered as a system user"
        }

        val apiKey = ApiKeyService.generate()
        val tenant = tenantRepository.save(
            TenantEntity(
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
                tenantId = tenant.id,
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
                    tenantId = tenant.id,
                    programId = program.id,
                    name = it.name.uppercase(),
                    rank = it.rank,
                    thresholdPoints = it.thresholdPoints,
                    multiplier = it.multiplier
                )
            }
        )

        val hostSponsor = sponsorRepository.save(
            SponsorEntity(
                tenantId = tenant.id,
                programId = program.id,
                name = request.name,
                sponsorCode = "HOST_${request.slug.uppercase().replace('-', '_')}",
                sponsorType = "HOST",
                status = "ACTIVE"
            )
        )

        branchRuleRepository.save(
            BranchRuleEntity(
                tenantId = tenant.id,
                programId = program.id,
                scope = "PROGRAM",
                name = "Default purchase earning policy",
                eventType = "PURCHASE",
                rewardType = "PERCENTAGE",
                rewardValue = request.earningRate,
                redemptionEarnRate = request.earningRate.divide(java.math.BigDecimal(100)),
                recognitionEarnRate = request.earningRate.divide(java.math.BigDecimal(100)),
                isActive = true,
                priority = 0
            )
        )

        if (!branchRepository.existsByTenantIdAndCode(tenant.id, DEFAULT_BRANCH_CODE)) {
            branchRepository.save(
                BranchEntity(
                    tenantId = tenant.id,
                    code = DEFAULT_BRANCH_CODE,
                    name = "${request.name} Main Branch",
                    city = null,
                    status = "ACTIVE"
                )
            )
        }

        if (sponsorLocationRepository.findByTenantIdAndSponsorIdAndLocationCode(
                tenant.id,
                hostSponsor.id,
                DEFAULT_LOCATION_CODE
            ) == null
        ) {
            sponsorLocationRepository.save(
                SponsorLocationEntity(
                    tenantId = tenant.id,
                    sponsorId = hostSponsor.id,
                    locationName = "${request.name} Main Location",
                    locationCode = DEFAULT_LOCATION_CODE,
                    locationPin = "DEFAULT",
                    status = "ACTIVE"
                )
            )
        }

        val username = request.adminEmail.substringBefore('@').ifBlank { request.slug }
            .lowercase()
            .replace(Regex("[^a-z0-9._-]"), "")
            .ifBlank { "admin_${tenant.id}" }

        val rawPassword = adminPasswordOverride ?: buildString {
            append("Adm!")
            append(ApiKeyService.generate().replace("-", "a").replace("_", "B").take(12))
            append("9")
        }

        val passwordHash = passwordEncoder.encode(rawPassword) ?: error("Password encoding failed")
        val isTemporaryPassword = adminPasswordOverride == null
        val systemUser = systemUserRepository.save(
            SystemUserEntity(
                email = request.adminEmail.lowercase(),
                username = username,
                passwordHash = passwordHash,
                tenantId = tenant.id,
                programId = program.id,
                sponsorId = hostSponsor.id,
                role = role,
                status = "ACTIVE",
                forcePasswordChange = isTemporaryPassword
            )
        )

        return ProvisionedTenantResult(
            tenant = tenant,
            program = program,
            hostSponsor = hostSponsor,
            tiers = tiers,
            apiKey = apiKey,
            systemUser = systemUser,
            rawPassword = rawPassword
        )
    }
}