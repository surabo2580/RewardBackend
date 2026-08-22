package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "reward_tiers")
data class TierEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val tenantId: String = "",

    @Column(nullable = false)
    val programId: String = "",

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val rank: Int = 0,

    @Column(nullable = false)
    val thresholdPoints: Long = 0,

    @Column(nullable = false)
    val multiplier: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
