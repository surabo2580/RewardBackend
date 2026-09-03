package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.time.Instant

@Entity
@Table(name = "reward_accounts")
data class AccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val memberId: Long = 0,

    @Column(nullable = false)
    val accountType: String = "REDEMPTION",

    @Column(nullable = false)
    var availablePoints: Long = 0,

    @Column(nullable = false)
    var pendingPoints: Long = 0,

    @Column(nullable = false)
    var redeemedPoints: Long = 0,

    @Column(nullable = false, columnDefinition = "bigint default 0")
    var lifetimeEarnedPoints: Long = 0,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
