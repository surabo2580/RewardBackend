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
    name = "reward_member_links",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reward_member_link", columnNames = ["tenant_id", "primary_member_id", "linked_member_id"])
    ]
)
data class MemberLinkEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    @Column(nullable = false) val primaryMemberId: Long = 0,
    @Column(nullable = false) val linkedMemberId: Long = 0,
    // SPOUSE | CHILD | FAMILY | CORPORATE | MERGED
    @Column(nullable = false) val relationType: String = "FAMILY",
    @Column(nullable = false) val canSharePoints: Boolean = false,
    @Column(nullable = false) val createdAt: Instant = Instant.now()
)
