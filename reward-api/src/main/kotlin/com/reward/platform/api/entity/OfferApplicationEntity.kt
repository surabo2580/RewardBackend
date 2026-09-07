package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_offer_applications")
data class OfferApplicationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val tenantId: Long = 0,
    @Column(nullable = false)
    val offerId: Long = 0,
    @Column(nullable = false)
    val memberId: Long = 0,
    @Column(nullable = false)
    val transactionId: Long = 0,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)