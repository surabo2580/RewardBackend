package com.reward.platform.core.domain

import java.math.BigDecimal
import java.time.Instant

data class RewardRule(
    val id: String,
    val tenantId: String,
    val programId: String,
    val name: String,
    val eventType: String,
    val conditionType: RuleConditionType,
    val minAmount: BigDecimal? = null,
    val rewardType: RuleRewardType,
    val rewardValue: BigDecimal,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now()
)

enum class RuleConditionType {
    ALWAYS,
    MIN_AMOUNT,
    TIER,
    SEGMENT
}

enum class RuleRewardType {
    FLAT,
    PERCENTAGE,
    MULTIPLIER
}
