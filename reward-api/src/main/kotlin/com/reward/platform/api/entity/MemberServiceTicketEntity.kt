package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reward_member_service_tickets")
data class MemberServiceTicketEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    @Column(nullable = false) val memberId: Long = 0,
    @Column(nullable = false) val ticketReference: String = "",
    // NOTE | MISSING_POINTS | SERVICE_COMPLAINT | TIER_INQUIRY | GOODWILL | POINT_ADJUSTMENT | TIER_OVERRIDE | RESERVATION | F_AND_B
    @Column(nullable = false) val category: String = "NOTE",
    // LOW | MEDIUM | HIGH | URGENT
    @Column(nullable = false) val priority: String = "MEDIUM",
    // OPEN | IN_PROGRESS | RESOLVED | CLOSED
    @Column(nullable = false) val status: String = "OPEN",
    @Column(nullable = false) val subject: String = "",
    @Column(length = 4000) val description: String? = null,
    // Hotnotes are surfaced on the member portal landing page and pinned in the member workspace.
    @Column(nullable = false) val isHotnote: Boolean = false,
    val createdByUserId: Long? = null,
    @Column(nullable = false) val pointsAdjusted: Long = 0,
    @Column(length = 4000) val resolutionNotes: String? = null,
    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    val resolvedAt: Instant? = null
)
