package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "reward_branch_rules")
data class BranchRuleEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val tenantId: String = "",

    val branchId: String? = null,
    val programId: String? = null,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val eventType: String = "PURCHASE",

    val minAmount: BigDecimal? = null,

    @Column(nullable = false)
    val rewardType: String = "FLAT",

    @Column(nullable = false)
    val rewardValue: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
