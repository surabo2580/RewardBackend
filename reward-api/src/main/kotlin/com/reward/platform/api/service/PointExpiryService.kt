package com.reward.platform.api.service

import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.TenantRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PointExpiryService(
    private val tenantRepository: TenantRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository
) {
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    fun runNightlyExpiry() {
        val now = Instant.now()
        tenantRepository.findByStatus("ACTIVE").forEach { tenant ->
            walletHistoryRepository.findExpirable(tenant.id, now)
                .map { it.memberId }
                .distinct()
                .forEach { memberId -> expireMemberPoints(tenant.id, memberId, now) }
        }
    }

    fun expireMemberPoints(tenantId: Long, memberId: Long, now: Instant = Instant.now()) {
        val account = accountRepository.findLockedByTenantIdAndMemberIdAndAccountType(tenantId, memberId, "REDEMPTION") ?: return
        val expiredLots = walletHistoryRepository.findLockedSpendableCredits(tenantId, memberId)
            .filter { it.expiresAt != null && !it.expiresAt.isAfter(now) }
        val pointsToExpire = expiredLots.sumOf { it.remainingPoints }.coerceAtMost(account.availablePoints)
        if (pointsToExpire <= 0) return

        var remainingToExpire = pointsToExpire
        expiredLots.forEach { lot ->
            val expiredFromLot = minOf(lot.remainingPoints, remainingToExpire)
            if (expiredFromLot > 0) {
                val remainingPoints = lot.remainingPoints - expiredFromLot
                walletHistoryRepository.save(lot.copy(
                    remainingPoints = remainingPoints,
                    isExpired = remainingPoints == 0L,
                    expiredAt = if (remainingPoints == 0L) now else null
                ))
                remainingToExpire -= expiredFromLot
            }
        }

        val updatedAccount = accountRepository.save(account.copy(
            availablePoints = account.availablePoints - pointsToExpire,
            updatedAt = now
        ))
        val transaction = transactionRepository.save(TransactionEntity(
            tenantId = tenantId,
            memberId = memberId,
            accountId = updatedAccount.id,
            eventType = "EXPIRY",
            transactionType = "EXPIRE",
            points = pointsToExpire,
            status = "APPROVED",
            channel = "SCHEDULED_JOB",
            createdAt = now
        ))
        walletHistoryRepository.save(WalletHistoryEntity(
            tenantId = tenantId,
            memberId = memberId,
            accountId = updatedAccount.id,
            accountType = "REDEMPTION",
            entryType = "DEBIT",
            points = pointsToExpire,
            description = "Points expired under the program expiry policy",
            createdAt = now
        ))
    }
}