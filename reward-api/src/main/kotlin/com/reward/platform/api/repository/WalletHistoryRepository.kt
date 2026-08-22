package com.reward.platform.api.repository

import com.reward.platform.api.entity.WalletHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface WalletHistoryRepository : JpaRepository<WalletHistoryEntity, String> {
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: String, memberId: String): List<WalletHistoryEntity>
    fun findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: String, branchId: String): List<WalletHistoryEntity>
}
