package com.smartReward.backend.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventRequest(
    val businessId: String = "",
    val userId: String = "",

    @JsonAlias("event", "type")
    val eventType: String = "",

    val amount: Double = 0.0,
    val referenceId: String? = null,
    val properties: Map<String, Any>? = emptyMap()
) {
    val event: String
        get() = eventType
}

data class EventResponse(
    val success: Boolean = true,
    val pointsAwarded: Double = 0.0,
    val matchedRulesCount: Int = 0,
    val transactionId: Long? = null,
    val message: String = "",
    val pendingPoints: Double = 0.0,
    val availablePoints: Double = 0.0
)