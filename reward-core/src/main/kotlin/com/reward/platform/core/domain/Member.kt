package com.reward.platform.core.domain

import java.time.Instant

data class Member(
    val id: String,
    val tenantId: String,
    val externalUserId: String,
    val email: String? = null,
    val tier: String = "STANDARD",
    val createdAt: Instant = Instant.now()
)
