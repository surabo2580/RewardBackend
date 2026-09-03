package com.reward.platform.api.controller

import com.reward.platform.api.dto.ReversalRequest
import com.reward.platform.api.dto.ReversalResponse
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.MemberRepository
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
import java.math.RoundingMode
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/transactions")
class ReversalController(
    private val accountRepository: AccountRepository,
    private val memberRepository: MemberRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository
) {

    @PostMapping("/reverse")
    @Transactional
    fun reverse(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @Valid @RequestBody request: ReversalRequest
    ): ResponseEntity<ReversalResponse> {
        require(request.tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        require(request.reversalPercentage in 1..100) { "Reversal percentage must be between 1 and 100" }

        val reversalReference = request.referenceId.trim()
        transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(
            request.tenantId,
            reversalReference,
            "REVERSAL"
        )?.let { return ResponseEntity.ok(existingResponse(it)) }

        val original = transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(
            request.tenantId,
            request.originalReferenceId.trim(),
            "EARN"
        ) ?: return ResponseEntity.badRequest().body(
            ReversalResponse(false, "ORIGINAL_NOT_FOUND", redemptionPointsReversed = 0, recognitionPointsReversed = 0, message = "Original earn transaction not found")
        )
        require(transactionRepository.findByTenantIdAndOriginalTransactionIdAndTransactionType(request.tenantId, original.id, "REVERSAL") == null) {
            "Original transaction has already been reversed"
        }

        val member = memberRepository.findLockedByIdAndTenantId(original.memberId, request.tenantId)
            ?: return ResponseEntity.badRequest().body(
                ReversalResponse(false, "MEMBER_NOT_FOUND", redemptionPointsReversed = 0, recognitionPointsReversed = 0, message = "Member for original transaction not found")
            )
        val redemptionAccount = accountRepository.findLockedByTenantIdAndMemberIdAndAccountType(request.tenantId, member.id, "REDEMPTION")
            ?: return ResponseEntity.badRequest().body(
                ReversalResponse(false, "ACCOUNT_NOT_FOUND", redemptionPointsReversed = 0, recognitionPointsReversed = 0, message = "Redemption account not found")
            )
        val recognitionAccount = accountRepository.findLockedByTenantIdAndMemberIdAndAccountType(request.tenantId, member.id, "RECOGNITION")
            ?: return ResponseEntity.badRequest().body(
                ReversalResponse(false, "ACCOUNT_NOT_FOUND", redemptionPointsReversed = 0, recognitionPointsReversed = 0, message = "Recognition account not found")
            )

        val redemptionPoints = proportionalPoints(original.points, request.reversalPercentage)
        val recognitionPoints = proportionalPoints(original.recognitionPoints, request.reversalPercentage)
        val updatedRedemptionAccount = accountRepository.save(redemptionAccount.copy(
            availablePoints = redemptionAccount.availablePoints - redemptionPoints,
            updatedAt = Instant.now()
        ))
        val updatedRecognitionAccount = accountRepository.save(recognitionAccount.copy(
            availablePoints = recognitionAccount.availablePoints - recognitionPoints,
            updatedAt = Instant.now()
        ))
        val transaction = transactionRepository.save(
            TransactionEntity(
                tenantId = request.tenantId,
                programId = original.programId,
                sponsorId = original.sponsorId,
                locationId = original.locationId,
                branchId = original.branchId,
                memberId = member.id,
                accountId = updatedRedemptionAccount.id,
                eventType = "REVERSAL",
                transactionType = "REVERSAL",
                amount = proportionalPoints(original.amount, request.reversalPercentage),
                points = redemptionPoints,
                recognitionPoints = recognitionPoints,
                policyId = original.policyId,
                policyScope = original.policyScope,
                status = "APPROVED",
                referenceId = reversalReference,
                originalTransactionId = original.id,
                channel = request.channel?.ifBlank { "POS" } ?: "POS"
            )
        )
        val reason = request.reason.trim()
        walletHistoryRepository.save(WalletHistoryEntity(
            tenantId = request.tenantId, programId = original.programId, sponsorId = original.sponsorId,
            locationId = original.locationId, branchId = original.branchId, memberId = member.id,
            accountId = updatedRedemptionAccount.id, accountType = "REDEMPTION", entryType = "DEBIT",
            points = redemptionPoints, policyId = original.policyId, policyScope = original.policyScope,
            description = "REVERSAL of ${original.referenceId}: $reason"
        ))
        walletHistoryRepository.save(WalletHistoryEntity(
            tenantId = request.tenantId, programId = original.programId, sponsorId = original.sponsorId,
            locationId = original.locationId, branchId = original.branchId, memberId = member.id,
            accountId = updatedRecognitionAccount.id, accountType = "RECOGNITION", entryType = "DEBIT",
            points = recognitionPoints, policyId = original.policyId, policyScope = original.policyScope,
            description = "REVERSAL of ${original.referenceId}: $reason"
        ))

        return ResponseEntity.ok(ReversalResponse(
            true, "SUCCESS", transaction.id, redemptionPoints, recognitionPoints,
            "Reversed $redemptionPoints redemption and $recognitionPoints recognition points"
        ))
    }

    private fun proportionalPoints(points: Long, percentage: Int): Long = BigDecimal.valueOf(points)
        .multiply(BigDecimal.valueOf(percentage.toLong()))
        .divide(BigDecimal(100), 0, RoundingMode.DOWN)
        .longValueExact()

    private fun existingResponse(transaction: TransactionEntity) = ReversalResponse(
        true, "ALREADY_PROCESSED", transaction.id, transaction.points, transaction.recognitionPoints,
        "This reversal reference was already processed"
    )
}