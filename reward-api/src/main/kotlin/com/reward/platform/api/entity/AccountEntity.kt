package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_accounts")
data class AccountEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val tenantId: String = "",

    @Column(nullable = false)
    val memberId: String = "",

    @Column(nullable = false)
    val accountType: String = "EARN_REDEEM",

    @Column(nullable = false)
    var availablePoints: Long = 0,

    @Column(nullable = false)
    var pendingPoints: Long = 0,

    @Column(nullable = false)
    var redeemedPoints: Long = 0,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
