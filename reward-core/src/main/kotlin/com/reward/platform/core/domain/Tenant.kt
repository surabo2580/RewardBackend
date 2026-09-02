package com.reward.platform.core.domain

import java.time.Instant

data class Tenant(
    val id: String,
    val name: String,
    val slug: String,
    val baseUrl: String,
    val schemaName: String,
    val adminEmail: String,
    val status: TenantStatus = TenantStatus.ACTIVE,
    val createdAt: Instant = Instant.now()
)

enum class TenantStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
