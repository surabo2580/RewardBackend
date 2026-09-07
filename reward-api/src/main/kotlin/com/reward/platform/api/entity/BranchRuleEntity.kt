package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "reward_branch_rules")
data class BranchRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    val branchId: Long? = null,
    val programId: Long? = null,
    val sponsorId: Long? = null,
    val locationId: Long? = null,

    @Column(nullable = false)
    val scope: String = "PROGRAM",

    @Column(nullable = false)
    val priority: Int = 0,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val eventType: String = "PURCHASE",

    val minAmount: BigDecimal? = null,

    @Column(nullable = false)
    val rewardType: String = "FLAT",

    @Column(nullable = false)
    val rewardValue: BigDecimal = BigDecimal.ZERO,

    val redemptionEarnRate: BigDecimal? = null,

    val recognitionEarnRate: BigDecimal? = null,

    @Column(nullable = false)
    val isActive: Boolean = true,

    val validFrom: Instant? = null,

    val validUntil: Instant? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
