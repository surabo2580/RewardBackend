package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_transactions")
data class TransactionEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val tenantId: String = "",

    @Column(nullable = false)
    val memberId: String = "",

    @Column(nullable = false)
    val accountId: String = "",

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
    val createdAt: Instant = Instant.now()
)
