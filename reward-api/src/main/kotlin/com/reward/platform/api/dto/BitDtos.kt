package com.reward.platform.api.dto

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class BitResponse(
    val id: Long,
    val bitReference: String,
    val bitType: String,
    val bitTypeLabel: String,
    val bitCategory: String,
    val status: String,
    val errorCode: String?,
    val errorMessage: String?,
    val memberId: Long,
    val programId: Long?,
    val bitSponsorId: Long?,
    val bitSponsorName: String?,
    val billingSponsorId: Long?,
    val billingSponsorName: String?,
    val locationId: Long?,
    val branchId: Long?,
    val channel: String,
    val bitSource: String?,
    val grossAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val netAmount: BigDecimal,
    val currency: String?,
    val redemptionPointsDelta: Long,
    val recognitionPointsDelta: Long,
    val appliedPolicyId: Long?,
    val appliedOfferIds: List<Long>,
    val originalBitId: Long?,
    val description: String?,
    val payload: Map<String, Any?>?,
    val createdByUserId: Long?,
    val interactionAt: Instant,
    val createdAt: Instant
)

data class BitLedgerEntry(
    val transactionId: Long,
    val transactionType: String,
    val points: Long,
    val recognitionPoints: Long,
    val status: String,
    val createdAt: Instant
)

data class BitDetailResponse(
    val bit: BitResponse,
    val ledger: List<BitLedgerEntry>,
    val reversals: List<BitResponse>,
    val offers: List<BitOfferSummary> = emptyList(),
    val vouchers: List<BitVoucherSummary> = emptyList()
)

data class BitOfferSummary(val id: Long, val offerCode: String, val name: String, val category: String)

data class BitVoucherSummary(val id: Long, val offerId: Long, val voucherCode: String, val status: String, val expiresAt: Instant?)

data class BitListRowResponse(
    val bitId: Long,
    val bitReference: String,
    val interactionDate: Instant,
    val sponsorName: String?,
    val memberCode: String,
    val bitCategory: String,
    val bitType: String,
    val bitTypeLabel: String,
    val offerName: String?,
    val pointsDelta: Long?,
    val redemptionPoints: Long,
    val recognitionPoints: Long,
    val rewardsEarned: Int,
    val rewardsAvailed: Int?,
    val status: String,
    val errorCode: String?,
    val errorMessage: String?,
    val source: String
)

data class BitPageResponse(
    val items: List<BitListRowResponse>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
)

data class BitCreateRequest(
    @field:jakarta.validation.constraints.NotBlank(message = "Member id is required")
    val memberId: String = "",
    @field:jakarta.validation.constraints.NotBlank(message = "BIT type is required")
    val bitType: String = "",
    @field:jakarta.validation.constraints.NotBlank(message = "Reference id is required for idempotency")
    val referenceId: String = "",
    val programId: Long? = null,
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    val channel: String? = null,
    @field:jakarta.validation.constraints.DecimalMin(value = "0", message = "Amount must be zero or greater")
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String? = null,
    val interactionAt: Instant? = null,
    val description: String? = null,
    val payload: Map<String, Any?>? = null
)

data class BitDayCount(val date: LocalDate, val count: Long)

data class BitSponsorCount(val sponsorId: Long?, val sponsorName: String?, val count: Long)

data class MemberBitSummaryResponse(
    val totalBits: Long,
    val firstBitAt: Instant?,
    val lastBitAt: Instant?,
    val daysSinceLastBit: Long?,
    val lastBits: List<BitResponse>,
    val countsByType: Map<String, Long>,
    val countsByCategory: Map<String, Long>,
    val topSponsors: List<BitSponsorCount>,
    val span: List<BitDayCount>
)
