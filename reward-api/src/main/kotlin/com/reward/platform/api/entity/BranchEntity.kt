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
    name = "reward_branches",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reward_branches_tenant_code", columnNames = ["tenant_id", "code"])
    ]
)
data class BranchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: Long = 0,

    val parentBranchId: Long? = null,

    @Column(nullable = false)
    val code: String = "",

    @Column(nullable = false)
    val name: String = "",

    val city: String? = null,

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
