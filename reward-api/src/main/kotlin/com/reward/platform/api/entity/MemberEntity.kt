package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.time.Instant

@Entity
@Table(name = "reward_members")
data class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val externalUserId: String = "",

    val email: String? = null,

    @Column(nullable = false)
    val tier: String = "STANDARD",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
