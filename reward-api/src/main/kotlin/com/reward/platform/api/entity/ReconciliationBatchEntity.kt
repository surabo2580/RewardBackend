package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "reward_reconciliation_batches")
data class ReconciliationBatchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long = 0,

    @Column(name = "sponsor_id", nullable = false)
    val sponsorId: Long = 0,

    @Column(nullable = false)
    val periodStart: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val periodEnd: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val pointCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val totalPoints: Long = 0,

    @Column(nullable = false)
    val totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val lineCount: Int = 0,

    @Column(nullable = false)
    val status: String = "OPEN",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)