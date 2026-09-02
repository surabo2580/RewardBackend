package com.reward.platform.core.domain

import java.time.Instant

data class Transaction(
    val id: String,
    val tenantId: String,
    val branchId: String? = null,
    val memberId: String,
    val programId: String,
    val type: TransactionType,
    val eventType: String,
    val amount: Long = 0,
    val points: Long = 0,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val referenceId: String? = null,
    val createdAt: Instant = Instant.now()
)

enum class TransactionType {
    EARN,
    REDEEM,
    ADJUSTMENT,
    EXPIRE
}

enum class TransactionStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED,
    FAILED
}
