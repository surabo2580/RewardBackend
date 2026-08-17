package com.smartReward.backend.service

import com.smartReward.backend.dto.EventRequest
import com.smartReward.backend.dto.EventResponse
import com.smartReward.backend.model.Transaction
import com.smartReward.backend.model.Wallet
import com.smartReward.backend.repository.RewardRuleRepository
import com.smartReward.backend.repository.TransactionRepository
import com.smartReward.backend.repository.WalletRepository
import com.smartReward.backend.ruleengine.RuleEngine
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventService(
    private val rewardRuleRepository: RewardRuleRepository,
    private val ruleEngine: RuleEngine,
    private val walletService: WalletService,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional
    fun processEvent(event: EventRequest): EventResponse {
        val eventTypeStr = if (event.eventType.isNotBlank()) event.eventType else event.event

        val rules = rewardRuleRepository.findByBusinessIdAndEventTypeAndIsActiveTrue(
            businessId = event.businessId,
            eventType = eventTypeStr
        )

        val pointsAwarded = ruleEngine.evaluateRules(rules, event)

        if (pointsAwarded > 0) {
            val wallet = walletRepository
                .findByUserIdAndBusinessId(event.userId, event.businessId)
                ?: Wallet(userId = event.userId, businessId = event.businessId)

            val updatedWallet = wallet.copy(
                pendingPoints = wallet.pendingPoints + pointsAwarded
            )
            walletRepository.save(updatedWallet)

            val tx = Transaction(
                userId = event.userId,
                businessId = event.businessId,
                eventType = eventTypeStr,
                points = pointsAwarded,
                status = "PENDING",
                referenceId = event.referenceId ?: "EVT-${System.currentTimeMillis()}"
            )
            val savedTx = transactionRepository.save(tx)

            val matchedRules = rules.filter { 
                val min = it.minAmount
                min == null || event.amount >= min 
            }

            return EventResponse(
                success = true,
                pointsAwarded = pointsAwarded.toDouble(),
                matchedRulesCount = matchedRules.size,
                transactionId = savedTx.id,
                message = "Event processed. Awarded $pointsAwarded points to pending balance.",
                pendingPoints = updatedWallet.pendingPoints.toDouble(),
                availablePoints = updatedWallet.availablePoints.toDouble()
            )
        }

        val wallet = walletService.getWallet(event.userId, event.businessId)
        return EventResponse(
            success = true,
            pointsAwarded = 0.0,
            matchedRulesCount = 0,
            transactionId = null,
            message = "Event processed, no matching active rules.",
            pendingPoints = wallet.pendingPoints.toDouble(),
            availablePoints = wallet.availablePoints.toDouble()
        )
    }
}