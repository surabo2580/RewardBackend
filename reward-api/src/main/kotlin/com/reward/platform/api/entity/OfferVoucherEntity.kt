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
@Table(name = "reward_offer_vouchers", uniqueConstraints = [UniqueConstraint(name = "uk_reward_voucher_code", columnNames = ["tenant_id", "voucher_code"])])
data class OfferVoucherEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    @Column(nullable = false) val offerId: Long = 0,
    @Column(nullable = false) val voucherCode: String = "",
    @Column(nullable = false) val isIssued: Boolean = false,
    val issuedToMemberId: Long? = null,
    val issuedAt: Instant? = null,
    val expiresAt: Instant? = null,
    val referenceId: String? = null
)