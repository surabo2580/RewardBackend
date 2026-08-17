package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_wallet_history")
data class WalletHistoryEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val tenantId: String = "",

    @Column(nullable = false)
    val memberId: String = "",

    @Column(nullable = false)
    val accountId: String = "",

    @Column(nullable = false)
    val entryType: String = "CREDIT",

    @Column(nullable = false)
    val points: Long = 0,

    val description: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
