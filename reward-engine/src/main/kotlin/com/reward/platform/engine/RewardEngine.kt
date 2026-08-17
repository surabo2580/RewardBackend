package com.reward.platform.engine

import com.reward.platform.core.domain.RewardRule
import com.reward.platform.core.domain.Transaction
import java.math.BigDecimal

class RewardEngine {

    fun evaluate(rule: RewardRule, amount: BigDecimal): Long {
        if (!rule.isActive) return 0

        return when (rule.rewardType) {
            com.reward.platform.core.domain.RuleRewardType.FLAT -> rule.rewardValue.toLong()
            com.reward.platform.core.domain.RuleRewardType.PERCENTAGE -> {
                val percent = rule.rewardValue.divide(BigDecimal("100"), 10, java.math.RoundingMode.HALF_UP)
                (amount.multiply(percent)).toLong()
            }
            com.reward.platform.core.domain.RuleRewardType.MULTIPLIER -> {
                (amount.multiply(rule.rewardValue)).toLong()
            }
        }
    }

    fun process(transaction: Transaction): Transaction {
        return transaction.copy(status = com.reward.platform.core.domain.TransactionStatus.APPROVED)
    }
}
