package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "reward_partner_memberships",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_reward_partner_membership_external",
            columnNames = ["tenant_id", "sponsor_id", "external_membership_id"]
        )
    ]
)
data class PartnerMembershipEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long = 0,

    @Column(name = "sponsor_id", nullable = false)
    val sponsorId: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long = 0,

    @Column(name = "external_membership_id", nullable = false)
    val externalMembershipId: String = "",

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)