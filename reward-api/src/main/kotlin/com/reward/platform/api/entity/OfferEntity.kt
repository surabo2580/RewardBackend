package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "reward_offers",
    indexes = [Index(name = "idx_reward_offers_tenant_active_window", columnList = "tenant_id,is_active,start_date,end_date")]
)
data class OfferEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val tenantId: Long = 0,
    @Column(nullable = false)
    val programId: Long = 0,
    @Column(nullable = false)
    val offerCode: String = "",
    @Column(nullable = false)
    val name: String = "",
    val description: String? = null,
    @Column(nullable = false)
    val category: String = "AWARD",
    @Column(nullable = false)
    val status: String = "DRAFT",
    @Column(nullable = false)
    val scope: String = "PROGRAM",
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    @Column(nullable = false)
    val offerType: String = "MULTIPLIER",
    @Column(nullable = false)
    val multiplier: BigDecimal = BigDecimal.ONE,
    @Column(nullable = false)
    val bonusPoints: Long = 0,
    @Column(nullable = false)
    val minSpend: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false)
    val minTierRank: Int = 0,
    val eligibleDays: String? = null,
    val maxUsesPerMember: Int? = null,
    val maxTotalClaims: Int? = null,
    @Column(nullable = false)
    val totalClaimsCount: Int = 0,
    @Column(nullable = false)
    val isMto: Boolean = false,
    @Column(nullable = false)
    val isFeatured: Boolean = false,
    @Column(nullable = false)
    val pointsRequired: Long = 0,
    val benefitCode: String? = null,
    val targetTierId: Long? = null,
    val discountType: String? = null,
    val discountValue: BigDecimal? = null,
    val promoCode: String? = null,
    @Column(nullable = false)
    val startDate: Instant = Instant.now(),
    @Column(nullable = false)
    val endDate: Instant = Instant.now().plusSeconds(31_536_000),
    @Column(nullable = false)
    val isActive: Boolean = true,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)