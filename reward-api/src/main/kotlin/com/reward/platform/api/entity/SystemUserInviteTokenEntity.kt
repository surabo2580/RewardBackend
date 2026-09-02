package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_system_user_invite_tokens")
data class SystemUserInviteTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long = 0,

    @Column(nullable = false, unique = true, length = 128)
    val tokenHash: String = "",

    @Column(nullable = false)
    val expiresAt: Instant = Instant.now(),

    val usedAt: Instant? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)