package com.smartReward.backend.model

import jakarta.persistence.*

@Entity
@Table(name = "transactions")
data class Transaction(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val userId: String,

    val businessId: String,

    val eventType: String,

    val points: Int,

    val status: String, // PENDING, CONFIRMED, REDEEMED, EXPIRED

    val referenceId: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)