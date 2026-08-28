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
@Table(name = "reward_tiers")
data class TierEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val programId: Long = 0,

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
