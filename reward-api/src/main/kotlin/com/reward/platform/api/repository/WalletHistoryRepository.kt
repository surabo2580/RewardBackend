package com.reward.platform.api.repository

import com.reward.platform.api.entity.WalletHistoryEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface WalletHistoryRepository : JpaRepository<WalletHistoryEntity, Long> {
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: Long, memberId: Long): List<WalletHistoryEntity>
    fun findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: Long, branchId: Long): List<WalletHistoryEntity>
    @Query("""
        select w from WalletHistoryEntity w
        where w.tenantId = :tenantId
          and w.accountType = 'REDEMPTION'
          and w.entryType = 'CREDIT'
          and w.isExpired = false
          and w.expiresAt is not null
          and w.expiresAt <= :now
          and w.remainingPoints > 0
    """)
    fun findExpirable(@Param("tenantId") tenantId: Long, @Param("now") now: Instant): List<WalletHistoryEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select w from WalletHistoryEntity w
        where w.tenantId = :tenantId
          and w.memberId = :memberId
          and w.accountType = 'REDEMPTION'
          and w.entryType = 'CREDIT'
          and w.isExpired = false
          and w.remainingPoints > 0
        order by w.expiresAt asc nulls last, w.createdAt asc
    """)
    fun findLockedSpendableCredits(
        @Param("tenantId") tenantId: Long,
        @Param("memberId") memberId: Long
    ): List<WalletHistoryEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select w from WalletHistoryEntity w
        where w.tenantId = :tenantId
          and w.memberId = :memberId
          and w.accountType = 'REDEMPTION'
          and w.entryType = 'CREDIT'
          and w.isExpired = false
          and w.remainingPoints > 0
          and (w.expiresAt is null or w.expiresAt > :now)
        order by w.expiresAt asc nulls last, w.createdAt asc
    """)
    fun findLockedUnexpiredSpendableCredits(
        @Param("tenantId") tenantId: Long,
        @Param("memberId") memberId: Long,
        @Param("now") now: Instant
    ): List<WalletHistoryEntity>
}
