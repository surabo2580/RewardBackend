package com.smartReward.backend.dto

data class EventRequest(
    val userId: String,
    val businessId: String,
    val event: String,
    val properties: Map<String, Any> = emptyMap()
)