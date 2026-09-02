package com.reward.platform.api.dto

import com.reward.platform.api.entity.ProgramEntity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class ProgramCreateRequest(
    val tenantId: Long = 0,

    @field:NotBlank(message = "Program name is required")
    val name: String = "",

    val currency: String = "USD",
    val timezone: String = "UTC",
    val status: String = "DRAFT",

    @field:NotNull(message = "Earning rate is required")
    val earningRate: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Redemption rate is required")
    val redemptionRate: BigDecimal = BigDecimal.ZERO
)

data class ProgramResponse(
    val id: Long,
    val tenantId: Long,
    val name: String,
    val currency: String,
    val timezone: String,
    val status: String,
    val createdAt: Instant,
    val earningRate: BigDecimal,
    val redemptionRate: BigDecimal
) {
    companion object {
        fun from(entity: ProgramEntity): ProgramResponse = ProgramResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            name = entity.name,
            currency = entity.currency,
            timezone = entity.timezone,
            status = entity.status,
            createdAt = entity.createdAt,
            earningRate = entity.earningRate,
            redemptionRate = entity.redemptionRate
        )
    }
}
