package com.smartReward.backend.repository

import com.smartReward.backend.model.RewardRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RewardRuleRepository : JpaRepository<RewardRule, Long> {
    fun findByBusinessIdAndEventTypeAndIsActiveTrue(
        businessId: String,
        eventType: String
    ): List<RewardRule>
}