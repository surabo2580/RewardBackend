package com.reward.platform.api.controller

import com.reward.platform.api.dto.RewardEventRequest
import com.reward.platform.api.dto.RewardEventResponse
import com.reward.platform.api.entity.AccountEntity
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.BranchRepository
import com.reward.platform.api.repository.BranchRuleRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api")
class RewardEventController(
    private val accountRepository: AccountRepository,
    private val branchRepository: BranchRepository,
    private val branchRuleRepository: BranchRuleRepository,
    private val memberRepository: MemberRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository
) {

    @PostMapping("/events")
    @Transactional
    fun processEvent(@Valid @RequestBody request: RewardEventRequest): ResponseEntity<RewardEventResponse> {
        val member = memberRepository.findByTenantIdAndExternalUserId(request.tenantId, request.memberId)
            ?: return ResponseEntity.badRequest().body(
                RewardEventResponse(success = false, pointsAwarded = 0, message = "Member not found")
            )

        val branch = request.branchCode?.let {
            branchRepository.findByTenantIdAndCode(request.tenantId, it)
                ?: return ResponseEntity.badRequest().body(
                    RewardEventResponse(success = false, pointsAwarded = 0, message = "Branch not found")
                )
        }

        val account = accountRepository.findByTenantIdAndMemberIdAndAccountType(
            tenantId = request.tenantId,
            memberId = member.id,
            accountType = "EARN_REDEEM"
        ) ?: AccountEntity(
            id = "acct-${System.currentTimeMillis()}",
            tenantId = request.tenantId,
            memberId = member.id,
            accountType = "EARN_REDEEM",
            availablePoints = 0,
            pendingPoints = 0,
            redeemedPoints = 0,
            updatedAt = Instant.now()
        )

        val eventType = request.eventType.uppercase()
        val configuredRules = branchRuleRepository
            .findByTenantIdAndEventTypeAndIsActiveTrue(request.tenantId, eventType)
            .filter { it.branchId == null || it.branchId == branch?.id }
            .filter { it.minAmount == null || BigDecimal.valueOf(request.amount) >= it.minAmount }

        val pointsAwarded = if (configuredRules.isNotEmpty()) {
            configuredRules.sumOf { rule ->
                when (rule.rewardType.uppercase()) {
                    "PERCENTAGE" -> BigDecimal.valueOf(request.amount)
                        .multiply(rule.rewardValue)
                        .divide(BigDecimal(100))
                        .toLong()
                    "MULTIPLIER" -> BigDecimal.valueOf(request.amount)
                        .multiply(rule.rewardValue)
                        .toLong()
                    else -> rule.rewardValue.toLong()
                }
            }
        } else {
            when (eventType) {
                "PURCHASE" -> if (request.amount > 0) request.amount / 10 else 10
                "SIGNUP" -> 100
                "REFERRAL" -> 250
                else -> 0
            }
        }

        val updatedAccount = account.copy(
            availablePoints = account.availablePoints + pointsAwarded,
            updatedAt = Instant.now()
        )
        accountRepository.save(updatedAccount)

        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            tenantId = request.tenantId,
            branchId = branch?.id,
            memberId = member.id,
            accountId = updatedAccount.id,
            eventType = eventType,
            transactionType = "EARN",
            amount = request.amount,
            points = pointsAwarded,
            status = "APPROVED",
            referenceId = request.referenceId,
            createdAt = Instant.now()
        )
        transactionRepository.save(transaction)

        val walletHistory = WalletHistoryEntity(
            id = UUID.randomUUID().toString(),
            tenantId = request.tenantId,
            branchId = branch?.id,
            memberId = member.id,
            accountId = updatedAccount.id,
            entryType = "CREDIT",
            points = pointsAwarded,
            description = "Awarded for $eventType${branch?.let { " at ${it.name}" } ?: ""}",
            createdAt = Instant.now()
        )
        walletHistoryRepository.save(walletHistory)

        return ResponseEntity.ok(
            RewardEventResponse(
                success = true,
                pointsAwarded = pointsAwarded,
                message = "Awarded $pointsAwarded points for ${request.eventType}"
            )
        )
    }
}