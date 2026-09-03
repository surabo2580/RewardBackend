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
    @Column(nullable = false, columnDefinition = "varchar(32) default 'REDEMPTION'") val accountType: String = "REDEMPTION",
    @Column(nullable = false) val entryType: String = "CREDIT",
    @Column(nullable = false) val points: Long = 0,
    val policyId: Long? = null,
    val policyScope: String? = null,
    val description: String? = null,
    val expiresAt: Instant? = null,
    val expiredAt: Instant? = null,
    @Column(nullable = false, columnDefinition = "bigint default 0") val remainingPoints: Long = 0,
    @Column(nullable = false, columnDefinition = "boolean default false") val isExpired: Boolean = false,
    @Column(nullable = false) val createdAt: Instant = Instant.now()
)
