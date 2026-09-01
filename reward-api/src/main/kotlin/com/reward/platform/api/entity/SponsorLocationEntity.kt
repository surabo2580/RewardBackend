package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import java.time.Instant

@Entity
@Table(
    name = "reward_sponsor_locations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reward_locations_tenant_sponsor_code", columnNames = ["tenant_id", "sponsor_id", "location_code"]),
        UniqueConstraint(name = "uk_reward_locations_tenant_sponsor_pin", columnNames = ["tenant_id", "sponsor_id", "location_pin"])
    ]
)
data class SponsorLocationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long = 0,

    @Column(name = "sponsor_id", nullable = false)
    val sponsorId: Long = 0,

    @Column(nullable = false)
    val locationName: String = "",

    @Column(name = "location_code", nullable = false)
    val locationCode: String = "",

    @Column(name = "location_pin", nullable = false)
    val locationPin: String = "",

    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)