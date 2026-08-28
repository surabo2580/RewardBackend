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

    @field:NotBlank(message = "Member id is required")
    val memberId: String = "",

    @field:NotBlank(message = "Event type is required")
    val eventType: String = "PURCHASE",

    @field:Min(value = 0, message = "Amount must be zero or greater")
    val amount: Long = 0,

    val referenceId: String? = null,
    val channel: String = "POS"
)

data class RewardEventResponse(
    val success: Boolean = true,
    val pointsAwarded: Long = 0,
    val message: String = ""
)
