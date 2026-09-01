package com.reward.platform.api.dto

import com.reward.platform.api.entity.BranchRuleEntity
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class BranchRuleCreateRequest(
    val tenantId: Long = 0,

    val branchId: Long? = null,
    val programId: Long = 0,
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    val scope: String = "PROGRAM", // PROGRAM | SPONSOR | LOCATION | PARENT | PARTNER

    @field:NotBlank(message = "Rule name is required")
    val name: String = "",

    @field:NotBlank(message = "Event type is required")
    val eventType: String = "PURCHASE",

    val minAmount: BigDecimal? = null,

    @field:NotBlank(message = "Reward type is required")
    val rewardType: String = "FLAT",

    val rewardValue: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val priority: Int = 0
)

data class BranchRuleResponse(
    val id: Long,
    val tenantId: Long,
    val branchId: Long?,
    val programId: Long?,
    val sponsorId: Long?,
    val locationId: Long?,
    val scope: String,
    val name: String,
    val eventType: String,
    val minAmount: BigDecimal?,
    val rewardType: String,
    val rewardValue: BigDecimal,
    val isActive: Boolean,
    val priority: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: BranchRuleEntity) = BranchRuleResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            branchId = entity.branchId,
            programId = entity.programId,
            sponsorId = entity.sponsorId,
            locationId = entity.locationId,
            scope = entity.scope,
            name = entity.name,
            eventType = entity.eventType,
            minAmount = entity.minAmount,
            rewardType = entity.rewardType,
            rewardValue = entity.rewardValue,
            isActive = entity.isActive,
            priority = entity.priority,
            createdAt = entity.createdAt
        )
    }
}
