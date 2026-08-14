package com.smartReward.backend.repository

import com.smartReward.backend.model.RewardRule
import org.springframework.data.jpa.repository.JpaRepository

interface RewardRuleRepository : JpaRepository<RewardRule, Long> {

    fun findByEventTypeAndBusinessId(
        eventType: String,
        tenantId: String
    ): List<RewardRule>
}