package com.smartReward.backend.model

import jakarta.persistence.*

@Entity
@Table(
    name = "wallets",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["userId", "businessId"])
    ]
)
data class Wallet(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val userId: String,

    val businessId: String,

    var availablePoints: Int = 0,

    var pendingPoints: Int = 0,

    var updatedAt: Long = System.currentTimeMillis()
)