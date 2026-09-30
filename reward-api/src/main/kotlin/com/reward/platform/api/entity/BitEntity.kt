package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant

enum class BitCategory {
    ACCRUAL, REDEMPTION, PRIVILEGE, DEAL, AVAILMENT, ENGAGEMENT, CANCELLATION,
    SERVICE, ENROLLMENT, PROFILE_UPDATE, TIER_CHANGE, EXPIRATION
}

enum class BitType(val category: BitCategory, val label: String) {
    PURCHASE(BitCategory.ACCRUAL, "Purchase"),
    DINING(BitCategory.ACCRUAL, "Dining"),
    ONLINE_ORDER(BitCategory.ACCRUAL, "Online Order"),
    REDEMPTION(BitCategory.REDEMPTION, "Point Redemption"),
    REWARD_CLAIM(BitCategory.REDEMPTION, "Reward Claim"),
    PRIVILEGE_CLAIM(BitCategory.PRIVILEGE, "Privilege Claim"),
    DEAL(BitCategory.DEAL, "Deal Applied"),
    HOTEL_STAY(BitCategory.ACCRUAL, "Hotel Stay"),
    FLIGHT_SEGMENT(BitCategory.ACCRUAL, "Flight Segment"),
    CAR_RENTAL(BitCategory.ACCRUAL, "Car Rental"),
    APP_LOGIN(BitCategory.ENGAGEMENT, "App Login"),
    SURVEY(BitCategory.ENGAGEMENT, "Survey"),
    REFERRAL(BitCategory.ENGAGEMENT, "Referral"),
    SOCIAL_SHARE(BitCategory.ENGAGEMENT, "Social Share"),
    EVENT(BitCategory.ENGAGEMENT, "Event"),
    PROMO_OFFERS(BitCategory.ENGAGEMENT, "Promo Offers"),
    REVERSAL(BitCategory.CANCELLATION, "Reversal"),
    CS_ADJUSTMENT(BitCategory.SERVICE, "CS Adjustment"),
    ENROLL(BitCategory.ENROLLMENT, "Enroll"),
    PROFILE_UPDATE(BitCategory.PROFILE_UPDATE, "Update Profile"),
    TIER_CHANGE(BitCategory.TIER_CHANGE, "Tier Change"),
    EXPIRATION(BitCategory.EXPIRATION, "Point Expiration");

    companion object {
        fun parse(value: String): BitType =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported BIT type '$value'. Allowed: ${entries.joinToString { it.name }}")
    }
}

/** Business Interaction Transaction: the member touchpoint that produces zero or more ledger entries. */
@Entity
@Table(
    name = "reward_bits",
    uniqueConstraints = [UniqueConstraint(name = "uk_reward_bits_tenant_ref_type", columnNames = ["tenant_id", "bit_reference", "bit_type"])],
    indexes = [
        Index(name = "idx_reward_bits_member_time", columnList = "tenant_id, member_id, interaction_at"),
        Index(name = "idx_reward_bits_sponsor_time", columnList = "tenant_id, bit_sponsor_id, interaction_at")
    ]
)
data class BitEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    val programId: Long? = null,
    @Column(nullable = false) val bitReference: String = "",
    @Column(nullable = false, length = 40) val bitType: String = BitType.PURCHASE.name,
    @Column(nullable = false, length = 20) val bitCategory: String = BitCategory.ACCRUAL.name,
    @Column(nullable = false) val memberId: Long = 0,
    // Outlet/partner where the interaction happened.
    val bitSponsorId: Long? = null,
    // Sponsor that funds the points/discount; defaults to the BIT sponsor.
    val billingSponsorId: Long? = null,
    val locationId: Long? = null,
    val branchId: Long? = null,
    @Column(nullable = false, length = 30) val channel: String = "POS",
    // COMPLETED | PENDING | REVERSED | PARTIALLY_REVERSED | REJECTED
    @Column(nullable = false, length = 20) val status: String = "COMPLETED",
    @Column(nullable = false, precision = 14, scale = 2) val grossAmount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, precision = 14, scale = 2) val discountAmount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false, precision = 14, scale = 2) val netAmount: BigDecimal = BigDecimal.ZERO,
    @Column(length = 10) val currency: String? = null,
    // Signed deltas: credits positive, debits negative.
    @Column(nullable = false) val redemptionPointsDelta: Long = 0,
    @Column(nullable = false) val recognitionPointsDelta: Long = 0,
    val appliedPolicyId: Long? = null,
    @Column(length = 500) val appliedOfferIds: String? = null,
    val originalBitId: Long? = null,
    @Column(length = 500) val description: String? = null,
    // Basket / segment / survey metadata as JSON.
    @Column(length = 8000) val payload: String? = null,
    val createdByUserId: Long? = null,
    @Column(nullable = false) val interactionAt: Instant = Instant.now(),
    @Column(nullable = false) val createdAt: Instant = Instant.now()
)
