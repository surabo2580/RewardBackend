package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_system_users")
data class SystemUserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String = "",

    @Column(nullable = false, unique = true)
    val username: String = "",

    @Column(nullable = false)
    val passwordHash: String = "",

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val programId: Long = 0,

    val sponsorId: Long? = null,

    @Column(nullable = false)
    val role: String = "TENANT_ADMIN",

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val forcePasswordChange: Boolean = false,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    val lastLoginAt: Instant? = null
)