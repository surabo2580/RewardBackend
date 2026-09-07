package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal

@Entity
@Table(
    name = "reward_offer_kpis",
    uniqueConstraints = [UniqueConstraint(name = "uk_reward_offer_kpi", columnNames = ["offer_id", "kpi_code"])]
)
data class OfferKpiEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val offerId: Long = 0,
    @Column(nullable = false) val kpiCode: String = "",
    @Column(nullable = false) val targetValue: BigDecimal = BigDecimal.ZERO
)
