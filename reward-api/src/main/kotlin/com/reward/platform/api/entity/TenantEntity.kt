package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_tenants")
data class TenantEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val name: String = "",

    @Column(unique = true)
    val slug: String? = null,

    @Column(unique = true)
    val baseUrl: String? = null,

    @Column(unique = true)
    val schemaName: String? = null,

    @Column(unique = true, length = 128)
    val apiKeyHash: String? = null,

    val adminEmail: String? = null,

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
