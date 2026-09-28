package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import org.hibernate.annotations.ColumnDefault
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "reward_members")
data class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val tenantId: Long = 0,

    @Column(nullable = false)
    val externalUserId: String = "",

    val email: String? = null,

    @Column(nullable = false)
    val tier: String = "STANDARD",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val alternatePhone: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: String? = null,
    val nationality: String? = null,
    val preferredLanguage: String? = null,
    val enrollingSponsorId: Long? = null,

    @Column(nullable = false, length = 32)
    @ColumnDefault("'ACTIVE'")
    val status: String = "ACTIVE"
)
