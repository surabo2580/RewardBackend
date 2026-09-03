package com.reward.platform.api.repository

import com.reward.platform.api.entity.TransactionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface TransactionRepository : JpaRepository<TransactionEntity, Long> {
    fun findByTenantIdAndReferenceIdAndTransactionType(
        tenantId: Long,
        referenceId: String,
        transactionType: String
    ): TransactionEntity?
    fun findByTenantIdAndOriginalTransactionIdAndTransactionType(
        tenantId: Long,
        originalTransactionId: Long,
        transactionType: String
    ): TransactionEntity?
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: Long, memberId: Long): List<TransactionEntity>
    fun findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: Long, branchId: Long): List<TransactionEntity>
    fun findByTenantIdAndSponsorIdAndTransactionTypeAndCreatedAtBetween(
        tenantId: Long,
        sponsorId: Long,
        transactionType: String,
        periodStart: Instant,
        periodEnd: Instant
    ): List<TransactionEntity>
}
