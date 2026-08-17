package com.smartReward.backend.ruleengine

import com.smartReward.backend.dto.EventRequest
import com.smartReward.backend.model.RewardRule
import org.springframework.stereotype.Component

@Component
class RuleEngine {

    fun evaluateRules(rules: List<RewardRule>, event: EventRequest): Int {
        return rules
            .filter { it.isActive }
            .filter { rule -> 
                val min = rule.minAmount
                min == null || event.amount >= min 
            }
            .sumOf { calculateReward(it, event) }
    }

    private fun calculateReward(rule: RewardRule, event: EventRequest): Int {
        return when (rule.rewardType.uppercase()) {
            "PERCENTAGE" -> ((event.amount * rule.rewardValue) / 100.0).toInt()
            "FLAT" -> rule.rewardValue.toInt()
            else -> 0
        }
    }
}