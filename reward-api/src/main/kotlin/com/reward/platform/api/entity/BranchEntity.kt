package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_branches")
data class BranchEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    val tenantId: String = "",

    val parentBranchId: String? = null,

    @Column(nullable = false, unique = true)
    val code: String = "",

    @Column(nullable = false)
    val name: String = "",

    val city: String? = null,

    @Column(nullable = false)
    val status: String = "ACTIVE",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
