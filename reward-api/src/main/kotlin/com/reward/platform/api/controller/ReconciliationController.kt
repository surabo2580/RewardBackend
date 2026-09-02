package com.reward.platform.api.controller

import com.reward.platform.api.dto.ReconciliationBatchCreateRequest
import com.reward.platform.api.dto.ReconciliationBatchResponse
import com.reward.platform.api.dto.ReconciliationLineResponse
import com.reward.platform.api.dto.ReconciliationRunResponse
import com.reward.platform.api.entity.ReconciliationBatchEntity
import com.reward.platform.api.entity.ReconciliationLineEntity
import com.reward.platform.api.repository.ReconciliationBatchRepository
import com.reward.platform.api.repository.ReconciliationLineRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.TransactionRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneOffset

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/reconciliation")
class ReconciliationController(
    private val sponsorRepository: SponsorRepository,
    private val transactionRepository: TransactionRepository,
    private val batchRepository: ReconciliationBatchRepository,
    private val lineRepository: ReconciliationLineRepository
) {

    @PostMapping("/batches")
    @Transactional
    fun createBatch(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: ReconciliationBatchCreateRequest
    ): ResponseEntity<ReconciliationRunResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        require(!request.periodEnd.isBefore(request.periodStart)) { "periodEnd must be >= periodStart" }

        val sponsor = sponsorRepository.findById(request.sponsorId).orElse(null)
        require(sponsor != null && sponsor.tenantId == request.tenantId) { "Sponsor does not belong to tenant" }
        require(sponsor.sponsorType == "PARTNER") { "Reconciliation is only supported for PARTNER sponsors" }

        val periodStart = request.periodStart.atStartOfDay().toInstant(ZoneOffset.UTC)
        val periodEnd = request.periodEnd.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1)

        val partnerTxns = transactionRepository.findByTenantIdAndSponsorIdAndTransactionTypeAndCreatedAtBetween(
            request.tenantId,
            request.sponsorId,
            "EARN",
            periodStart,
            periodEnd
        )

        val totalPoints = partnerTxns.sumOf { it.points }
        val totalAmount = request.pointCost
            .multiply(BigDecimal(totalPoints))
            .setScale(2, RoundingMode.HALF_UP)

        val batch = batchRepository.save(
            ReconciliationBatchEntity(
                tenantId = request.tenantId,
                sponsorId = request.sponsorId,
                periodStart = request.periodStart,
                periodEnd = request.periodEnd,
                pointCost = request.pointCost,
                totalPoints = totalPoints,
                totalAmount = totalAmount,
                lineCount = partnerTxns.size,
                status = "OPEN"
            )
        )

        val lines = lineRepository.saveAll(
            partnerTxns.map { txn ->
                ReconciliationLineEntity(
                    batchId = batch.id,
                    tenantId = txn.tenantId,
                    sponsorId = txn.sponsorId ?: request.sponsorId,
                    transactionId = txn.id,
                    memberId = txn.memberId,
                    points = txn.points,
                    pointCost = request.pointCost,
                    amount = request.pointCost
                        .multiply(BigDecimal(txn.points))
                        .setScale(2, RoundingMode.HALF_UP)
                )
            }
        )

        return ResponseEntity.ok(
            ReconciliationRunResponse(
                batch = ReconciliationBatchResponse.from(batch),
                lines = lines.map(ReconciliationLineResponse::from)
            )
        )
    }

    @GetMapping("/batches")
    fun listBatches(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<List<ReconciliationBatchResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        return ResponseEntity.ok(batchRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).map(ReconciliationBatchResponse::from))
    }

    @GetMapping("/batches/{batchId}/lines")
    fun listLines(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @PathVariable batchId: Long
    ): ResponseEntity<List<ReconciliationLineResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        val batch = batchRepository.findById(batchId).orElse(null)
        require(batch != null && batch.tenantId == tenantId) { "Batch does not belong to tenant" }
        return ResponseEntity.ok(lineRepository.findByBatchIdOrderByIdAsc(batchId).map(ReconciliationLineResponse::from))
    }
}