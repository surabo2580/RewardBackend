package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_onboarding_requests")
data class OnboardingRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val companyName: String = "",

    @Column(nullable = false)
    val contactName: String = "",

    @Column(nullable = false)
    val contactEmail: String = "",

    @Column(nullable = false)
    val requestedPlan: String = "ENTERPRISE",

    val companySize: String? = null,
    val expectedMonthlyMembers: Long? = null,
    val expectedMonthlyTransactions: Long? = null,

    @Column(columnDefinition = "text")
    val notes: String? = null,

    @Column(nullable = false)
    val customPricingRequired: Boolean = true,

    @Column(nullable = false)
    val status: String = "NEW",

    val tenantId: Long? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
)