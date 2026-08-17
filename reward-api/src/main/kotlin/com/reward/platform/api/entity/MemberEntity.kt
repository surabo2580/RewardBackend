package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_members")
data class MemberEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val tenantId: String = "",

    @Column(nullable = false)
    val externalUserId: String = "",

    val email: String? = null,

    @Column(nullable = false)
    val tier: String = "STANDARD",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
