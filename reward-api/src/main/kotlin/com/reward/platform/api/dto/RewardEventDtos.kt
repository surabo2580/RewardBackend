package com.reward.platform.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class RewardEventRequest(
    val tenantId: Long = 0,

    val programId: Long = 0,
    val sponsorId: Long? = null,
    val sponsorCode: String? = null,
    val locationId: Long? = null,
    val locationCode: String? = null,
    val branchCode: String? = null,

    val memberId: String? = null,
    val externalMembershipId: String? = null,

    @field:jakarta.validation.constraints.NotBlank(message = "Event type is required")
    val eventType: String = "PURCHASE",

    @field:Min(value = 0, message = "Amount must be zero or greater")
    val amount: Long = 0,

    @field:NotBlank(message = "Reference ID is required for idempotency")
    val referenceId: String = "",
    val channel: String? = null
)

data class RewardEventResponse(
    val success: Boolean = true,
    val pointsAwarded: Long = 0,
    val recognitionPointsAwarded: Long = 0,
    val policyId: Long? = null,
    val policyScope: String? = null,
    val currentTier: String? = null,
    val tierUpgraded: Boolean = false,
    val message: String = ""
)

data class RedemptionRequest(
    val tenantId: Long = 0,
    val programId: Long = 0,
    val sponsorId: Long = 0,
    val locationId: Long? = null,
    @field:NotBlank(message = "Member ID is required")
    val memberId: String = "",
    @field:Min(value = 1, message = "Points to redeem must be greater than zero")
    val pointsToRedeem: Long = 0,
    @field:NotBlank(message = "Reference ID is required for idempotency")
    val referenceId: String = "",
    val channel: String? = null
)

data class RedemptionResponse(
    val success: Boolean,
    val status: String,
    val transactionId: Long? = null,
    val pointsRedeemed: Long,
    val discountAmount: String,
    val remainingBalance: Long,
    val message: String
)

data class ReversalRequest(
    val tenantId: Long = 0,
    @field:NotBlank(message = "Original purchase reference ID is required")
    val originalReferenceId: String = "",
    @field:NotBlank(message = "Reversal reference ID is required for idempotency")
    val referenceId: String = "",
    @field:Min(value = 1, message = "Reversal percentage must be between 1 and 100")
    val reversalPercentage: Int = 100,
    @field:NotBlank(message = "Reason is required")
    val reason: String = "",
    val channel: String? = null
)

data class ReversalResponse(
    val success: Boolean,
    val status: String,
    val transactionId: Long? = null,
    val redemptionPointsReversed: Long,
    val recognitionPointsReversed: Long,
    val message: String
)
