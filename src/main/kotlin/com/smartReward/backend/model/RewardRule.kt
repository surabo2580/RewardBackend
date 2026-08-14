package com.smartReward.backend.model

import jakarta.persistence.*

@Entity
@Table(name = "reward_rules")
data class RewardRule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val businessId: String,

    val eventType: String, // PURCHASE, SIGNUP, REFERRAL

    val minAmount: Double? = null,

    val rewardType: String, // FLAT, PERCENTAGE

    val rewardValue: Double,

    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)