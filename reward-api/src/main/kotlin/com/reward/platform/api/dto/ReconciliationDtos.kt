package com.reward.platform.api.dto

import com.reward.platform.api.entity.ReconciliationBatchEntity
import com.reward.platform.api.entity.ReconciliationLineEntity
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class ReconciliationBatchCreateRequest(
    val tenantId: Long = 0,
    val sponsorId: Long = 0,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    @field:DecimalMin(value = "0.0", inclusive = false, message = "pointCost must be positive")
    val pointCost: BigDecimal
)

data class ReconciliationBatchResponse(
    val id: Long,
    val tenantId: Long,
    val sponsorId: Long,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val pointCost: BigDecimal,
    val totalPoints: Long,
    val totalAmount: BigDecimal,
    val lineCount: Int,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: ReconciliationBatchEntity): ReconciliationBatchResponse = ReconciliationBatchResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            sponsorId = entity.sponsorId,
            periodStart = entity.periodStart,
            periodEnd = entity.periodEnd,
            pointCost = entity.pointCost,
            totalPoints = entity.totalPoints,
            totalAmount = entity.totalAmount,
            lineCount = entity.lineCount,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}

data class ReconciliationLineResponse(
    val id: Long,
    val batchId: Long,
    val tenantId: Long,
    val sponsorId: Long,
    val transactionId: Long,
    val memberId: Long,
    val points: Long,
    val pointCost: BigDecimal,
    val amount: BigDecimal,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: ReconciliationLineEntity): ReconciliationLineResponse = ReconciliationLineResponse(
            id = entity.id,
            batchId = entity.batchId,
            tenantId = entity.tenantId,
            sponsorId = entity.sponsorId,
            transactionId = entity.transactionId,
            memberId = entity.memberId,
            points = entity.points,
            pointCost = entity.pointCost,
            amount = entity.amount,
            createdAt = entity.createdAt
        )
    }
}

data class ReconciliationRunResponse(
    val batch: ReconciliationBatchResponse,
    val lines: List<ReconciliationLineResponse>
)