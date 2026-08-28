package com.reward.platform.api.repository

import com.reward.platform.api.entity.TierEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TierRepository : JpaRepository<TierEntity, Long> {
    fun findByTenantIdAndProgramIdOrderByRank(tenantId: Long, programId: Long): List<TierEntity>
}
