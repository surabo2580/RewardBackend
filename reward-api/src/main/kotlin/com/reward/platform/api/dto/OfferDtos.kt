package com.reward.platform.api.dto

import com.reward.platform.api.entity.OfferEntity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.Instant

data class OfferCreateRequest(
    val tenantId: Long = 0,
    val programId: Long = 0,
    @field:NotBlank(message = "Offer name is required")
    val name: String = "",
    val description: String? = null,
    val scope: String = "PROGRAM",
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    val offerType: String = "MULTIPLIER",
    val multiplier: BigDecimal = BigDecimal.ONE,
    @field:PositiveOrZero(message = "Bonus points cannot be negative")
    val bonusPoints: Long = 0,
    val minSpend: BigDecimal = BigDecimal.ZERO,
    @field:PositiveOrZero(message = "Minimum tier rank cannot be negative")
    val minTierRank: Int = 0,
    val eligibleDays: String? = null,
    val maxUsesPerMember: Int? = null,
    val startDate: Instant = Instant.now(),
    val endDate: Instant = Instant.now().plusSeconds(31_536_000),
    val isActive: Boolean = true
)

data class OfferResponse(
    val id: Long,
    val tenantId: Long,
    val programId: Long,
    val name: String,
    val description: String?,
    val scope: String,
    val sponsorId: Long?,
    val locationId: Long?,
    val offerType: String,
    val multiplier: BigDecimal,
    val bonusPoints: Long,
    val minSpend: BigDecimal,
    val minTierRank: Int,
    val eligibleDays: String?,
    val maxUsesPerMember: Int?,
    val startDate: Instant,
    val endDate: Instant,
    val isActive: Boolean
) {
    companion object {
        fun from(entity: OfferEntity) = OfferResponse(
            entity.id, entity.tenantId, entity.programId, entity.name, entity.description,
            entity.scope, entity.sponsorId, entity.locationId, entity.offerType, entity.multiplier,
            entity.bonusPoints, entity.minSpend, entity.minTierRank, entity.eligibleDays,
            entity.maxUsesPerMember, entity.startDate, entity.endDate, entity.isActive
        )
    }
}