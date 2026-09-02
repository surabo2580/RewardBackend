package com.reward.platform.api.repository

import com.reward.platform.api.entity.WalletHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface WalletHistoryRepository : JpaRepository<WalletHistoryEntity, Long> {
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: Long, memberId: Long): List<WalletHistoryEntity>
    fun findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: Long, branchId: Long): List<WalletHistoryEntity>
}
