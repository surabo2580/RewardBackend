package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_wallet_history")
data class WalletHistoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    val programId: Long? = null,
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    val branchId: Long? = null,
    @Column(nullable = false) val memberId: Long = 0,
    @Column(nullable = false) val accountId: Long = 0,
    @Column(nullable = false) val entryType: String = "CREDIT",
    @Column(nullable = false) val points: Long = 0,
    val description: String? = null,
    @Column(nullable = false) val createdAt: Instant = Instant.now()
)
