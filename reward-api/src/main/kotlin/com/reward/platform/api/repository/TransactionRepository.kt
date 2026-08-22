package com.reward.platform.api.repository

import com.reward.platform.api.entity.TransactionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<TransactionEntity, String> {
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: String, memberId: String): List<TransactionEntity>
    fun findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: String, branchId: String): List<TransactionEntity>
}
