package com.reward.platform.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class TierSetup(
    @field:NotBlank
    val name: String = "",
    val rank: Int = 0,
    val thresholdPoints: Long = 0,
    val multiplier: BigDecimal = BigDecimal.ONE
)

data class TenantProvisionRequest(
    @field:NotBlank
    val name: String = "",

    @field:NotBlank
    val slug: String = "",

    @field:Email
    @field:NotBlank
    val adminEmail: String = "",

    @field:NotBlank
    val programName: String = "",

    val currency: String? = "INR",
    val timezone: String? = "Asia/Kolkata",
    val earningRate: BigDecimal = BigDecimal.TEN,
    val redemptionRate: BigDecimal = BigDecimal.ONE,

    @field:Valid
    val tiers: List<TierSetup>? = null
)

data class TenantProvisionResponse(
    val tenant: TenantResponse,
    val program: ProgramResponse,
    val hostSponsor: SponsorResponse? = null,
    val tiers: List<TierResponse>,
    val apiKey: String,
    val apiKeyHeader: String = "X-API-Key"
)

data class TierResponse(
    val id: Long,
    val tenantId: Long,
    val programId: Long,
    val name: String,
    val rank: Int,
    val thresholdPoints: Long,
    val multiplier: BigDecimal,
    val createdAt: Instant
)
