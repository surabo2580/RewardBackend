package com.reward.platform.core.domain

import java.time.Instant

data class Account(
    val id: String,
    val tenantId: String,
    val memberId: String,
    val accountType: AccountType,
    val availablePoints: Long = 0,
    val pendingPoints: Long = 0,
    val redeemedPoints: Long = 0,
    val updatedAt: Instant = Instant.now()
)

enum class AccountType {
    EARN_REDEEM,
    RECOGNITION,
    EXPIRATION
}
