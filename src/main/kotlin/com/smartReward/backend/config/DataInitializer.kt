package com.smartReward.backend.config

import com.smartReward.backend.model.RewardRule
import com.smartReward.backend.repository.RewardRuleRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataInitializer {

    @Bean
    fun seedData(rewardRuleRepository: RewardRuleRepository): CommandLineRunner {
        return CommandLineRunner {
            if (rewardRuleRepository.count() == 0L) {
                val rules = listOf(
                    // Taj Luxury Hotels: 10% points on purchases over $50
                    RewardRule(
                        businessId = "taj",
                        eventType = "PURCHASE",
                        minAmount = 50.0,
                        rewardType = "PERCENTAGE",
                        rewardValue = 10.0,
                        isActive = true
                    ),
                    // Taj Signup Bonus: 100 flat points
                    RewardRule(
                        businessId = "taj",
                        eventType = "SIGNUP",
                        minAmount = 0.0,
                        rewardType = "FLAT",
                        rewardValue = 100.0,
                        isActive = true
                    ),
                    // Taj Referral Bonus: 250 flat points
                    RewardRule(
                        businessId = "taj",
                        eventType = "REFERRAL",
                        minAmount = 0.0,
                        rewardType = "FLAT",
                        rewardValue = 250.0,
                        isActive = true
                    )
                )

                rewardRuleRepository.saveAll(rules)
                println(">>> [DataInitializer] Successfully seeded ${rules.size} Reward Rules into database!")
            }
        }
    }
}