package com.reward.platform.api.repository

import com.reward.platform.api.entity.ReconciliationLineEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReconciliationLineRepository : JpaRepository<ReconciliationLineEntity, Long> {
    fun findByBatchIdOrderByIdAsc(batchId: Long): List<ReconciliationLineEntity>
}