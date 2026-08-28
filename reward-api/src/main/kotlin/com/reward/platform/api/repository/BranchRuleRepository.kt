package com.reward.platform.api.repository

import com.reward.platform.api.entity.BranchRuleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BranchRuleRepository : JpaRepository<BranchRuleEntity, Long> {
    fun findByTenantIdAndEventTypeAndIsActiveTrue(
        tenantId: Long,
        eventType: String
    ): List<BranchRuleEntity>
}
