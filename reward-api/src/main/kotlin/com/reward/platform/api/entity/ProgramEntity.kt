package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GenerationType
import jakarta.persistence.GeneratedValue
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "reward_programs")
data class ProgramEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val currency: String = "USD",

    @Column(nullable = false)
    val timezone: String = "UTC",

    @Column(nullable = false)
    val status: String = "DRAFT",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val earningRate: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val redemptionRate: BigDecimal = BigDecimal.ZERO
)
