package com.reward.platform.api.controller

import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.entity.AccountEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class RewardEventRequest(
    val tenantId: String = "",
    val memberId: String = "",
    val eventType: String = "PURCHASE",
    val amount: Long = 0,
    val referenceId: String? = null
)

data class RewardEventResponse(
    val success: Boolean = true,
    val pointsAwarded: Long = 0,
    val message: String = ""
)

@RestController
@RequestMapping("/api")
class RewardEventController(
    private val accountRepository: AccountRepository,
    private val memberRepository: MemberRepository
) {

    @PostMapping("/events")
    fun processEvent(@RequestBody request: RewardEventRequest): ResponseEntity<RewardEventResponse> {
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

        return ResponseEntity.ok(
            RewardEventResponse(
                success = true,
                pointsAwarded = pointsAwarded,
                message = "Awarded $pointsAwarded points for ${request.eventType}"
            )
        )
    }
}
