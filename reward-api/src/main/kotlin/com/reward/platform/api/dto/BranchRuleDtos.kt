package com.reward.platform.api.dto

import com.reward.platform.api.entity.BranchRuleEntity
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class BranchRuleCreateRequest(
    @field:NotBlank(message = "Tenant id is required")
    val tenantId: String = "",

    val branchId: String? = null,
    val programId: String? = null,

    @field:NotBlank(message = "Rule name is required")
    val name: String = "",

    @field:NotBlank(message = "Event type is required")
    val eventType: String = "PURCHASE",

    val minAmount: BigDecimal? = null,

    @field:NotBlank(message = "Reward type is required")
    val rewardType: String = "FLAT",

    val rewardValue: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true
)

data class BranchRuleResponse(
    val id: String,
    val tenantId: String,
    val branchId: String?,
    val programId: String?,
    val name: String,
    val eventType: String,
    val minAmount: BigDecimal?,
    val rewardType: String,
    val rewardValue: BigDecimal,
    val isActive: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: BranchRuleEntity) = BranchRuleResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            branchId = entity.branchId,
            programId = entity.programId,
            name = entity.name,
            eventType = entity.eventType,
            minAmount = entity.minAmount,
            rewardType = entity.rewardType,
            rewardValue = entity.rewardValue,
            isActive = entity.isActive,
            createdAt = entity.createdAt
        )
    }
}
