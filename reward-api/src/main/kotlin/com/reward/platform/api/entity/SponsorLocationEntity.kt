package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.time.Instant

@Entity
@Table(name = "reward_sponsor_locations")
data class SponsorLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val sponsorId: Long = 0,

    @Column(nullable = false)
    val locationName: String = "",

    @Column(nullable = false)
    val locationCode: String = "",

    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)