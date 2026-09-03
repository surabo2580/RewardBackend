package com.reward.platform.api.dto

import com.reward.platform.api.entity.OfferEntity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.Instant

data class OfferCreateRequest(
    val tenantId: Long = 0,
    val programId: Long = 0,
    @field:NotBlank(message = "Offer code is required")
    val offerCode: String = "",
    @field:NotBlank(message = "Offer name is required")
    val name: String = "",
    val description: String? = null,
    val category: String = "AWARD",
    val status: String = "DRAFT",
    val scope: String = "PROGRAM",
    val sponsorId: Long? = null,
    val sponsorIds: List<Long> = emptyList(),
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
    val maxTotalClaims: Int? = null,
    val isMto: Boolean = false,
    val isFeatured: Boolean = false,
    val pointsRequired: Long = 0,
    val benefitCode: String? = null,
    val targetTierId: Long? = null,
    val discountType: String? = null,
    val discountValue: BigDecimal? = null,
    val promoCode: String? = null,
    val targetMemberIds: List<Long> = emptyList(),
    val startDate: Instant = Instant.now(),
    val endDate: Instant = Instant.now().plusSeconds(31_536_000),
    val isActive: Boolean = true
)

data class OfferResponse(
    val id: Long,
    val tenantId: Long,
    val programId: Long,
    val offerCode: String,
    val name: String,
    val description: String?,
    val category: String,
    val status: String,
    val scope: String,
    val sponsorId: Long?,
    val sponsorIds: List<Long>,
    val locationId: Long?,
    val offerType: String,
    val multiplier: BigDecimal,
    val bonusPoints: Long,
    val minSpend: BigDecimal,
    val minTierRank: Int,
    val eligibleDays: String?,
    val maxUsesPerMember: Int?,
    val maxTotalClaims: Int?,
    val totalClaimsCount: Int,
    val isMto: Boolean,
    val isFeatured: Boolean,
    val pointsRequired: Long,
    val benefitCode: String?,
    val targetTierId: Long?,
    val discountType: String?,
    val discountValue: BigDecimal?,
    val promoCode: String?,
    val startDate: Instant,
    val endDate: Instant,
    val isActive: Boolean
) {
    companion object {
        fun from(entity: OfferEntity, sponsorIds: List<Long> = entity.sponsorId?.let(::listOf) ?: emptyList()) = OfferResponse(
            entity.id, entity.tenantId, entity.programId, entity.offerCode, entity.name, entity.description, entity.category, entity.status,
            entity.scope, entity.sponsorId, sponsorIds, entity.locationId, entity.offerType, entity.multiplier,
            entity.bonusPoints, entity.minSpend, entity.minTierRank, entity.eligibleDays,
            entity.maxUsesPerMember, entity.maxTotalClaims, entity.totalClaimsCount, entity.isMto, entity.isFeatured,
            entity.pointsRequired, entity.benefitCode, entity.targetTierId, entity.discountType, entity.discountValue, entity.promoCode,
            entity.startDate, entity.endDate, entity.isActive
        )
    }
}

data class OfferStatusUpdateRequest(
    @field:NotBlank(message = "Offer status is required")
    val status: String = ""
)

data class RewardClaimRequest(
    @field:NotBlank(message = "Member ID is required") val memberId: String = "",
    @field:NotBlank(message = "Reference ID is required") val referenceId: String = ""
)

data class RewardClaimResponse(
    val success: Boolean,
    val status: String,
    val transactionId: Long? = null,
    val offerName: String,
    val pointsBurned: Long,
    val voucherCode: String? = null,
    val remainingBalance: Long,
    val message: String
)

data class PrivilegeClaimRequest(
    @field:NotBlank(message = "Member ID is required") val memberId: String = "",
    val partnerProof: String? = null,
    @field:NotBlank(message = "Reference ID is required") val referenceId: String = ""
)

data class PrivilegeClaimResponse(
    val success: Boolean,
    val status: String,
    val transactionId: Long? = null,
    val offerName: String,
    val previousTier: String,
    val currentTier: String,
    val benefitUnlocked: String? = null,
    val message: String
)

data class DealRedemptionRequest(
    val tenantId: Long = 0,
    val programId: Long = 0,
    @field:NotBlank(message = "Member ID is required") val memberId: String = "",
    @field:NotBlank(message = "Offer code is required") val offerCode: String = "",
    @field:PositiveOrZero(message = "Bill amount cannot be negative") val billAmount: Long = 0,
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    @field:NotBlank(message = "Reference ID is required") val referenceId: String = ""
)

data class DealRedemptionResponse(
    val success: Boolean,
    val status: String,
    val transactionId: Long? = null,
    val offerCode: String,
    val originalAmount: Long,
    val discountAmount: String,
    val netPayableAmount: String,
    val message: String
)