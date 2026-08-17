package com.reward.platform.api.controller

import com.reward.platform.api.dto.RewardEventRequest
import com.reward.platform.api.dto.RewardEventResponse
import com.reward.platform.api.entity.AccountEntity
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api")
class RewardEventController(
    private val accountRepository: AccountRepository,
    private val memberRepository: MemberRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository
) {

    @PostMapping("/events")
    fun processEvent(@Valid @RequestBody request: RewardEventRequest): ResponseEntity<RewardEventResponse> {
        val member = memberRepository.findByTenantIdAndExternalUserId(request.tenantId, request.memberId)
            ?: return ResponseEntity.badRequest().body(
                RewardEventResponse(success = false, pointsAwarded = 0, message = "Member not found")
            )

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

        val pointsAwarded = when (request.eventType.uppercase()) {
            "PURCHASE" -> request.amount / 10
            "SIGNUP" -> 100
            "REFERRAL" -> 250
            else -> 0
        }

        val updatedAccount = account.copy(
            availablePoints = account.availablePoints + pointsAwarded,
            updatedAt = Instant.now()
        )
        accountRepository.save(updatedAccount)

        val transaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            tenantId = request.tenantId,
            memberId = member.id,
            accountId = updatedAccount.id,
            eventType = request.eventType,
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
            memberId = member.id,
            accountId = updatedAccount.id,
            entryType = "CREDIT",
            points = pointsAwarded,
            description = "Awarded for ${request.eventType}",
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
