package com.reward.platform.api.controller

import com.reward.platform.api.dto.RedemptionRequest
import com.reward.platform.api.dto.RedemptionResponse
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/transactions")
class RedemptionController(
    private val accountRepository: AccountRepository,
    private val memberRepository: MemberRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository
) {

    @PostMapping("/redeem")
    @Transactional
    fun redeem(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @Valid @RequestBody request: RedemptionRequest
    ): ResponseEntity<RedemptionResponse> {
        require(request.tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        require(programRepository.findById(request.programId).orElse(null)?.tenantId == request.tenantId) {
            "Program does not belong to tenant"
        }
        val sponsor = sponsorRepository.findById(request.sponsorId).orElse(null)
        require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) {
            "Sponsor does not belong to tenant and program"
        }
        request.locationId?.let { locationId ->
            val location = locationRepository.findById(locationId).orElse(null)
            require(location != null && location.tenantId == request.tenantId && location.sponsorId == sponsor.id) {
                "Location must belong to sponsor"
            }
        }

        val existing = transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(
            request.tenantId, request.referenceId.trim(), "REDEEM"
        )
        if (existing != null) {
            return ResponseEntity.ok(existingResponse(existing))
        }

        val member = memberRepository.findByTenantIdAndExternalUserId(request.tenantId, request.memberId.trim())
            ?: return ResponseEntity.badRequest().body(
                RedemptionResponse(false, "MEMBER_NOT_FOUND", pointsRedeemed = 0, discountAmount = "0", remainingBalance = 0, message = "Member not found")
            )
        val account = accountRepository.findLockedByTenantIdAndMemberIdAndAccountType(
            request.tenantId, member.id, "REDEMPTION"
        ) ?: return ResponseEntity.badRequest().body(
            RedemptionResponse(false, "ACCOUNT_NOT_FOUND", pointsRedeemed = 0, discountAmount = "0", remainingBalance = 0, message = "Member has no redemption account")
        )

        // Recheck after acquiring the member account lock so retries cannot debit twice.
        transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(request.tenantId, request.referenceId.trim(), "REDEEM")?.let {
            return ResponseEntity.ok(existingResponse(it))
        }

        if (account.availablePoints < request.pointsToRedeem) {
            return ResponseEntity.badRequest().body(
                RedemptionResponse(
                    success = false,
                    status = "INSUFFICIENT_BALANCE",
                    pointsRedeemed = 0,
                    discountAmount = "0",
                    remainingBalance = account.availablePoints,
                    message = "Insufficient redemption points. Available: ${account.availablePoints}"
                )
            )
        }

        val program = programRepository.findById(request.programId).orElseThrow()
        val discountAmount = BigDecimal.valueOf(request.pointsToRedeem).multiply(program.redemptionRate)
        val pointLots = walletHistoryRepository.findLockedUnexpiredSpendableCredits(
            request.tenantId,
            member.id,
            Instant.now()
        )
        if (pointLots.sumOf { it.remainingPoints } < request.pointsToRedeem) {
            return ResponseEntity.badRequest().body(
                RedemptionResponse(
                    success = false,
                    status = "INSUFFICIENT_BALANCE",
                    pointsRedeemed = 0,
                    discountAmount = "0",
                    remainingBalance = account.availablePoints,
                    message = "Insufficient unexpired redemption points. Run point expiry processing and retry."
                )
            )
        }
        var remainingToConsume = request.pointsToRedeem
        pointLots.forEach { pointLot ->
            if (remainingToConsume > 0) {
                val consumedPoints = minOf(pointLot.remainingPoints, remainingToConsume)
                walletHistoryRepository.save(pointLot.copy(remainingPoints = pointLot.remainingPoints - consumedPoints))
                remainingToConsume -= consumedPoints
            }
        }
        val updatedAccount = accountRepository.save(
            account.copy(
                availablePoints = account.availablePoints - request.pointsToRedeem,
                redeemedPoints = account.redeemedPoints + request.pointsToRedeem,
                updatedAt = Instant.now()
            )
        )
        val transaction = transactionRepository.save(
            TransactionEntity(
                tenantId = request.tenantId,
                programId = request.programId,
                sponsorId = sponsor.id,
                locationId = request.locationId,
                memberId = member.id,
                accountId = updatedAccount.id,
                eventType = "REDEMPTION",
                transactionType = "REDEEM",
                amount = 0,
                points = request.pointsToRedeem,
                discountAmount = discountAmount,
                status = "APPROVED",
                referenceId = request.referenceId.trim(),
                channel = request.channel?.ifBlank { "POS" } ?: "POS"
            )
        )
        walletHistoryRepository.save(
            WalletHistoryEntity(
                tenantId = request.tenantId,
                programId = request.programId,
                sponsorId = sponsor.id,
                locationId = request.locationId,
                memberId = member.id,
                accountId = updatedAccount.id,
                accountType = "REDEMPTION",
                entryType = "DEBIT",
                points = request.pointsToRedeem,
                description = "REDEMPTION debit at checkout; discount value $discountAmount"
            )
        )

        return ResponseEntity.ok(
            RedemptionResponse(
                success = true,
                status = "SUCCESS",
                transactionId = transaction.id,
                pointsRedeemed = request.pointsToRedeem,
                discountAmount = discountAmount.toPlainString(),
                remainingBalance = updatedAccount.availablePoints,
                message = "Redeemed ${request.pointsToRedeem} points for a $discountAmount discount"
            )
        )
    }

    private fun existingResponse(transaction: TransactionEntity): RedemptionResponse = RedemptionResponse(
        success = true,
        status = "ALREADY_PROCESSED",
        transactionId = transaction.id,
        pointsRedeemed = transaction.points,
        discountAmount = transaction.discountAmount?.toPlainString() ?: "0",
        remainingBalance = 0,
        message = "This redemption reference was already processed"
    )
}
