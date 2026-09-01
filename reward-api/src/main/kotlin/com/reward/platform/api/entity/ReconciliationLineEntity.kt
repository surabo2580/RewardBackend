package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "reward_reconciliation_lines",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reward_recon_batch_txn", columnNames = ["batch_id", "transaction_id"])
    ]
)
data class ReconciliationLineEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "batch_id", nullable = false)
    val batchId: Long = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long = 0,

    @Column(name = "sponsor_id", nullable = false)
    val sponsorId: Long = 0,

    @Column(name = "transaction_id", nullable = false)
    val transactionId: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long = 0,

    @Column(nullable = false)
    val points: Long = 0,

    @Column(nullable = false)
    val pointCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val amount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)