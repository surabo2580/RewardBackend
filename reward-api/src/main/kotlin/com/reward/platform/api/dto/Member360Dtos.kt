package com.reward.platform.api.dto

import com.reward.platform.api.entity.MemberBookingEntity
import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.entity.MemberServiceTicketEntity
import com.reward.platform.api.entity.MembershipCardEntity
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class MemberSearchResult(
    val id: Long,
    val externalUserId: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val dateOfBirth: LocalDate?,
    val tier: String,
    val status: String,
    val accountBalance: Long
)

data class MemberProfileResponse(
    val id: Long,
    val tenantId: Long,
    val externalUserId: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val alternatePhone: String?,
    val dateOfBirth: LocalDate?,
    val gender: String?,
    val nationality: String?,
    val preferredLanguage: String?,
    val enrollingSponsorId: Long?,
    val enrollingSponsorName: String?,
    val tier: String,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: MemberEntity, enrollingSponsorName: String?) = MemberProfileResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            externalUserId = entity.externalUserId,
            email = entity.email,
            firstName = entity.firstName,
            lastName = entity.lastName,
            phone = entity.phone,
            alternatePhone = entity.alternatePhone,
            dateOfBirth = entity.dateOfBirth,
            gender = entity.gender,
            nationality = entity.nationality,
            preferredLanguage = entity.preferredLanguage,
            enrollingSponsorId = entity.enrollingSponsorId,
            enrollingSponsorName = enrollingSponsorName,
            tier = entity.tier,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}

data class MemberProfileUpdateRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val alternatePhone: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val nationality: String? = null,
    val preferredLanguage: String? = null,
    val enrollingSponsorId: Long? = null,
    val status: String? = null
)

data class TierSummaryResponse(
    val tierName: String,
    val tierRank: Int,
    val multiplier: BigDecimal,
    val thresholdPoints: Long,
    val nextTierName: String?,
    val nextTierThreshold: Long?,
    val pointsToNextTier: Long?,
    val progressPercent: Int
)

data class MemberBalancesResponse(
    val spendablePoints: Long,
    val pendingPoints: Long,
    val redeemedPoints: Long,
    val lifetimeEarnedPoints: Long,
    val recognitionPoints: Long,
    val lifetimeRecognitionPoints: Long,
    val pointsExpiringSoon: Long,
    val expiryWarningDays: Int,
    val nextExpiryDate: Instant?
)

data class MemberCountsResponse(
    val transactions: Long,
    val activeVouchers: Long,
    val eligibleOffers: Long,
    val bookings: Long,
    val openTickets: Long,
    val linkedMembers: Long,
    val activeCards: Long
)

data class Member360Response(
    val profile: MemberProfileResponse,
    val programId: Long?,
    val programName: String?,
    val currency: String?,
    val tier: TierSummaryResponse,
    val balances: MemberBalancesResponse,
    val counts: MemberCountsResponse,
    val hotnotes: List<ServiceTicketResponse>
)

data class PointLotResponse(
    val id: Long,
    val accountType: String,
    val entryType: String,
    val points: Long,
    val remainingPoints: Long,
    val description: String?,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val expiredAt: Instant?,
    val lotStatus: String
) {
    companion object {
        fun from(entity: WalletHistoryEntity, now: Instant): PointLotResponse {
            val lotStatus = when {
                entity.entryType == "DEBIT" -> "DEBIT"
                entity.isExpired || (entity.expiresAt != null && entity.expiresAt <= now) -> "EXPIRED"
                entity.accountType == "REDEMPTION" && entity.remainingPoints == 0L -> "CONSUMED"
                else -> "ACTIVE"
            }
            return PointLotResponse(
                id = entity.id,
                accountType = entity.accountType,
                entryType = entity.entryType,
                points = entity.points,
                remainingPoints = entity.remainingPoints,
                description = entity.description,
                createdAt = entity.createdAt,
                expiresAt = entity.expiresAt,
                expiredAt = entity.expiredAt,
                lotStatus = lotStatus
            )
        }
    }
}

data class MemberBalanceDetailResponse(
    val balances: MemberBalancesResponse,
    val lots: List<PointLotResponse>
)

data class MemberTransactionResponse(
    val id: Long,
    val transactionType: String,
    val eventType: String,
    val status: String,
    val amount: Long,
    val points: Long,
    val recognitionPoints: Long,
    val offerBonusPoints: Long,
    val offerMultiplier: BigDecimal?,
    val discountAmount: BigDecimal?,
    val sponsorId: Long?,
    val sponsorName: String?,
    val locationId: Long?,
    val referenceId: String?,
    val channel: String,
    val policyScope: String?,
    val originalTransactionId: Long?,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: TransactionEntity, sponsorName: String?) = MemberTransactionResponse(
            id = entity.id,
            transactionType = entity.transactionType,
            eventType = entity.eventType,
            status = entity.status,
            amount = entity.amount,
            points = entity.points,
            recognitionPoints = entity.recognitionPoints,
            offerBonusPoints = entity.offerBonusPoints,
            offerMultiplier = entity.offerMultiplier,
            discountAmount = entity.discountAmount,
            sponsorId = entity.sponsorId,
            sponsorName = sponsorName,
            locationId = entity.locationId,
            referenceId = entity.referenceId,
            channel = entity.channel,
            policyScope = entity.policyScope,
            originalTransactionId = entity.originalTransactionId,
            createdAt = entity.createdAt
        )
    }
}

data class MemberVoucherResponse(
    val id: Long,
    val voucherCode: String,
    val offerId: Long,
    val offerName: String?,
    val offerCategory: String?,
    val issuedAt: Instant?,
    val expiresAt: Instant?,
    val referenceId: String?,
    val status: String
)

data class MemberOfferResponse(
    val id: Long,
    val offerCode: String,
    val name: String,
    val description: String?,
    val category: String,
    val offerType: String,
    val status: String,
    val isMto: Boolean,
    val isTargeted: Boolean,
    val multiplier: BigDecimal,
    val bonusPoints: Long,
    val pointsRequired: Long,
    val minTierRank: Int,
    val maxUsesPerMember: Int?,
    val usesByMember: Long,
    val startDate: Instant,
    val endDate: Instant,
    val eligible: Boolean,
    val ineligibleReason: String?
)

data class MemberLinkResponse(
    val id: Long,
    val linkSource: String,
    val relationType: String,
    val canSharePoints: Boolean,
    val direction: String,
    val memberId: Long?,
    val externalUserId: String?,
    val email: String?,
    val tier: String?,
    val sponsorId: Long?,
    val sponsorName: String?,
    val status: String,
    val createdAt: Instant
)

data class MemberLinkCreateRequest(
    @field:NotBlank(message = "Linked member id or email is required")
    val linkedMemberIdentifier: String = "",
    val relationType: String = "FAMILY",
    val canSharePoints: Boolean = false
)

data class MembershipCardResponse(
    val id: Long,
    val cardNumber: String,
    val cardType: String,
    val status: String,
    val barcodePayload: String,
    val issuedAt: Instant,
    val expiresAt: Instant?
) {
    companion object {
        fun from(entity: MembershipCardEntity) = MembershipCardResponse(
            id = entity.id,
            cardNumber = entity.cardNumber,
            cardType = entity.cardType,
            status = entity.status,
            barcodePayload = entity.barcodePayload,
            issuedAt = entity.issuedAt,
            expiresAt = entity.expiresAt
        )
    }
}

data class MembershipCardIssueRequest(
    val cardType: String = "DIGITAL",
    val validityMonths: Int? = 36,
    val replaceExisting: Boolean = false
)

data class StatusUpdateRequest(
    @field:NotBlank(message = "Status is required")
    val status: String = ""
)

data class MemberBookingResponse(
    val id: Long,
    val bookingReference: String,
    val sponsorId: Long?,
    val sponsorName: String?,
    val locationId: Long?,
    val bookingType: String,
    val status: String,
    val checkInDate: LocalDate?,
    val checkOutDate: LocalDate?,
    val nights: Long?,
    val roomType: String?,
    val roomNumber: String?,
    val totalAmount: BigDecimal,
    val currency: String,
    val pointsEarned: Long,
    val notes: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: MemberBookingEntity, sponsorName: String?) = MemberBookingResponse(
            id = entity.id,
            bookingReference = entity.bookingReference,
            sponsorId = entity.sponsorId,
            sponsorName = sponsorName,
            locationId = entity.locationId,
            bookingType = entity.bookingType,
            status = entity.status,
            checkInDate = entity.checkInDate,
            checkOutDate = entity.checkOutDate,
            nights = if (entity.checkInDate != null && entity.checkOutDate != null) {
                java.time.temporal.ChronoUnit.DAYS.between(entity.checkInDate, entity.checkOutDate)
            } else null,
            roomType = entity.roomType,
            roomNumber = entity.roomNumber,
            totalAmount = entity.totalAmount,
            currency = entity.currency,
            pointsEarned = entity.pointsEarned,
            notes = entity.notes,
            createdAt = entity.createdAt
        )
    }
}

data class MemberBookingCreateRequest(
    @field:NotBlank(message = "Booking reference is required")
    val bookingReference: String = "",
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    val bookingType: String = "HOTEL_STAY",
    val status: String = "CONFIRMED",
    val checkInDate: LocalDate? = null,
    val checkOutDate: LocalDate? = null,
    val roomType: String? = null,
    val roomNumber: String? = null,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String? = null,
    val notes: String? = null
)

data class ServiceTicketResponse(
    val id: Long,
    val memberId: Long,
    val ticketReference: String,
    val category: String,
    val priority: String,
    val status: String,
    val subject: String,
    val description: String?,
    val isHotnote: Boolean,
    val createdByUserId: Long?,
    val pointsAdjusted: Long,
    val resolutionNotes: String?,
    val createdAt: Instant,
    val resolvedAt: Instant?
) {
    companion object {
        fun from(entity: MemberServiceTicketEntity) = ServiceTicketResponse(
            id = entity.id,
            memberId = entity.memberId,
            ticketReference = entity.ticketReference,
            category = entity.category,
            priority = entity.priority,
            status = entity.status,
            subject = entity.subject,
            description = entity.description,
            isHotnote = entity.isHotnote,
            createdByUserId = entity.createdByUserId,
            pointsAdjusted = entity.pointsAdjusted,
            resolutionNotes = entity.resolutionNotes,
            createdAt = entity.createdAt,
            resolvedAt = entity.resolvedAt
        )
    }
}

data class HotnoteResponse(
    val ticket: ServiceTicketResponse,
    val memberExternalUserId: String?,
    val memberEmail: String?
)

data class ServiceTicketCreateRequest(
    @field:NotBlank(message = "Subject is required")
    val subject: String = "",
    val category: String = "NOTE",
    val priority: String = "MEDIUM",
    val description: String? = null,
    val isHotnote: Boolean = false
)

data class ServiceTicketUpdateRequest(
    val status: String? = null,
    val priority: String? = null,
    val resolutionNotes: String? = null,
    val isHotnote: Boolean? = null
)

data class PointAdjustmentRequest(
    // CREDIT | DEBIT
    @field:NotBlank(message = "Direction is required")
    val direction: String = "CREDIT",
    @field:Positive(message = "Points must be positive")
    val points: Long = 0,
    @field:NotBlank(message = "Reason is required")
    val reason: String = "",
    val category: String = "GOODWILL"
)

data class PointAdjustmentResponse(
    val transactionId: Long,
    val direction: String,
    val points: Long,
    val newBalance: Long,
    val ticket: ServiceTicketResponse
)

data class TierOverrideRequest(
    @field:NotBlank(message = "Target tier is required")
    val targetTier: String = "",
    @field:NotBlank(message = "Reason is required")
    val reason: String = ""
)

data class MonthlyActivity(
    val month: String,
    val spend: Long,
    val pointsEarned: Long,
    val pointsRedeemed: Long,
    val transactions: Long
)

data class MemberKpiResponse(
    val lifetimeSpend: Long,
    val earnTransactions: Long,
    val averageOrderValue: Long,
    val lifetimePointsEarned: Long,
    val lifetimePointsRedeemed: Long,
    val lifetimePointsExpired: Long,
    val redemptionRatePercent: Int,
    val visitsLast90Days: Long,
    val daysSinceLastActivity: Long?,
    val firstActivityAt: Instant?,
    val lastActivityAt: Instant?,
    val totalBookings: Long,
    val completedStays: Long,
    val totalNights: Long,
    val bookingRevenue: BigDecimal,
    val offersRedeemed: Long,
    val openTickets: Long,
    val tier: TierSummaryResponse,
    val monthly: List<MonthlyActivity>
)
