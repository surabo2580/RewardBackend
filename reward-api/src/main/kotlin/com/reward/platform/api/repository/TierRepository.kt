package com.reward.platform.api.repository

import com.reward.platform.api.entity.TierEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TierRepository : JpaRepository<TierEntity, String> {
    fun findByTenantIdAndProgramIdOrderByRank(tenantId: String, programId: String): List<TierEntity>
}
