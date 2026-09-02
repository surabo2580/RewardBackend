package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.time.Instant

@Entity
@Table(name = "reward_sponsors")
data class SponsorEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val programId: Long = 0,

    val parentSponsorId: Long? = null,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val sponsorCode: String = "",

    @Column(nullable = false)
    val sponsorType: String = "CHILD",

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)