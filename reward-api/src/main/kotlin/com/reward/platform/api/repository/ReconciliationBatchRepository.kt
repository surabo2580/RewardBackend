package com.reward.platform.api.repository

import com.reward.platform.api.entity.ReconciliationBatchEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReconciliationBatchRepository : JpaRepository<ReconciliationBatchEntity, Long> {
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: Long): List<ReconciliationBatchEntity>
}