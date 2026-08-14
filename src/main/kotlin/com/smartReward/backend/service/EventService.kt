package com.smartReward.backend.service

import com.smartReward.backend.dto.EventRequest
import com.smartReward.backend.model.Transaction
import com.smartReward.backend.model.Wallet
import com.smartReward.backend.repository.TransactionRepository
import com.smartReward.backend.repository.WalletRepository
import com.smartReward.backend.ruleengine.RuleEngine
import org.springframework.stereotype.Service

@Service
class EventService(
    private val ruleEngine: RuleEngine,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {

    fun processEvent(request: EventRequest) {
        val points = ruleEngine.evaluate(request)

        val wallet = walletRepository
            .findByUserIdAndBusinessId(request.userId, request.businessId)
            ?: Wallet(
                userId = request.userId,
                businessId = request.businessId
            )

        wallet.pendingPoints += points
        walletRepository.save(wallet)

        transactionRepository.save(
            Transaction(
                userId = request.userId,
                businessId = request.businessId,
                eventType = request.event,
                points = points,
                status = "PENDING"
            )
        )
    }
}