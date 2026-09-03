package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "reward_transactions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_reward_transactions_tenant_reference_type",
            columnNames = ["tenant_id", "reference_id", "transaction_type"]
        )
    ]
)
data class TransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    val programId: Long? = null,
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    val branchId: Long? = null,

    @Column(nullable = false)
    val memberId: Long = 0,

    @Column(nullable = false)
    val accountId: Long = 0,

    @Column(nullable = false)
    val eventType: String = "PURCHASE",

    @Column(nullable = false)
    val transactionType: String = "EARN",

    @Column(nullable = false)
    val amount: Long = 0,

    @Column(nullable = false)
    val points: Long = 0,

    val discountAmount: BigDecimal? = null,

    @Column(nullable = false, columnDefinition = "bigint default 0")
    val recognitionPoints: Long = 0,

    val policyId: Long? = null,

    val policyScope: String? = null,

    val offerMultiplier: BigDecimal? = null,

    @Column(nullable = false, columnDefinition = "bigint default 0")
    val offerBonusPoints: Long = 0,

    @Column(nullable = false)
    val status: String = "APPROVED",

    val referenceId: String? = null,

    val originalTransactionId: Long? = null,

    @Column(nullable = false)
    val channel: String = "POS",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
