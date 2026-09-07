package com.reward.platform.api.service

import com.reward.platform.api.repository.WalletHistoryRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RedemptionLotService(
    private val walletHistoryRepository: WalletHistoryRepository
) {
    fun consume(tenantId: Long, memberId: Long, points: Long, now: Instant = Instant.now()): Boolean {
        val lots = walletHistoryRepository.findLockedUnexpiredSpendableCredits(tenantId, memberId, now)
        if (lots.sumOf { it.remainingPoints } < points) return false

        var remainingToConsume = points
        lots.forEach { lot ->
            if (remainingToConsume > 0) {
                val consumed = minOf(lot.remainingPoints, remainingToConsume)
                walletHistoryRepository.save(lot.copy(remainingPoints = lot.remainingPoints - consumed))
                remainingToConsume -= consumed
            }
        }
        return true
    }
}