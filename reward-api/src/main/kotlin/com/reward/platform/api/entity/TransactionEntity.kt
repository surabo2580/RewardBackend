package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.time.Instant

@Entity
@Table(name = "reward_transactions")
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

    @Column(nullable = false)
    val status: String = "APPROVED",

    val referenceId: String? = null,
    @Column(nullable = false)
    val channel: String = "POS",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
