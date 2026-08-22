package com.reward.platform.api.repository

import com.reward.platform.api.entity.BranchRuleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BranchRuleRepository : JpaRepository<BranchRuleEntity, String> {
    fun findByTenantIdAndEventTypeAndIsActiveTrue(
        tenantId: String,
        eventType: String
    ): List<BranchRuleEntity>
}
