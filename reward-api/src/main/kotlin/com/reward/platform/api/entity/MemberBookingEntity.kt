package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "reward_member_bookings",
    uniqueConstraints = [UniqueConstraint(name = "uk_reward_member_booking_ref", columnNames = ["tenant_id", "booking_reference"])]
)
data class MemberBookingEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    @Column(nullable = false) val memberId: Long = 0,
    @Column(nullable = false) val bookingReference: String = "",
    val sponsorId: Long? = null,
    val locationId: Long? = null,
    // HOTEL_STAY | DINING | SPA | EVENT | POS_RETAIL
    @Column(nullable = false) val bookingType: String = "HOTEL_STAY",
    // CONFIRMED | CHECKED_IN | COMPLETED | CANCELLED | NO_SHOW
    @Column(nullable = false) val status: String = "CONFIRMED",
    val checkInDate: LocalDate? = null,
    val checkOutDate: LocalDate? = null,
    val roomType: String? = null,
    val roomNumber: String? = null,
    @Column(nullable = false, precision = 14, scale = 2) val totalAmount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false) val currency: String = "INR",
    @Column(nullable = false) val pointsEarned: Long = 0,
    val notes: String? = null,
    @Column(nullable = false) val createdAt: Instant = Instant.now()
)
