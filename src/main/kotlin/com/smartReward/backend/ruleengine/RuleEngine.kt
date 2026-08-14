package com.smartReward.backend.ruleengine

import com.smartReward.backend.dto.EventRequest
import com.smartReward.backend.repository.RewardRuleRepository
import org.springframework.stereotype.Component

@Component
class RuleEngine(
    private val rewardRuleRepository: RewardRuleRepository
) {

    fun evaluate(event: EventRequest): Int {
        val rules = rewardRuleRepository
            .findByEventTypeAndBusinessId(event.event, event.businessId)

        var totalPoints = 0

        for (rule in rules) {

            val amount = (event.properties["amount"] as? Number)?.toDouble() ?: 0.0

            // Condition check
            if (rule.minAmount != null && amount < rule.minAmount!!) continue

            // Reward calculation
            val points = when (rule.rewardType) {
                "FLAT" -> rule.rewardValue.toInt()
                "PERCENTAGE" -> (amount * rule.rewardValue / 100).toInt()
                else -> 0
            }

            totalPoints += points
        }

        return totalPoints
    }
}