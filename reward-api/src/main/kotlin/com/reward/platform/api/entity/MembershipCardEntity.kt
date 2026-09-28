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
    name = "reward_membership_cards",
    uniqueConstraints = [UniqueConstraint(name = "uk_reward_membership_card_number", columnNames = ["tenant_id", "card_number"])]
)
data class MembershipCardEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    @Column(nullable = false) val memberId: Long = 0,
    @Column(nullable = false) val cardNumber: String = "",
    // DIGITAL | PHYSICAL | APPLE_WALLET | GOOGLE_WALLET
    @Column(nullable = false) val cardType: String = "DIGITAL",
    // ACTIVE | BLOCKED | EXPIRED | REPLACED
    @Column(nullable = false) val status: String = "ACTIVE",
    @Column(nullable = false) val barcodePayload: String = "",
    @Column(nullable = false) val issuedAt: Instant = Instant.now(),
    val expiresAt: Instant? = null
)
