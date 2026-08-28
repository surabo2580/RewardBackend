package com.reward.platform.api.repository

import com.reward.platform.api.entity.TransactionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<TransactionEntity, Long> {
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: Long, memberId: Long): List<TransactionEntity>
    fun findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: Long, branchId: Long): List<TransactionEntity>
}
