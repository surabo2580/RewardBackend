package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "reward_offer_locations",
    uniqueConstraints = [UniqueConstraint(name = "uk_reward_offer_location", columnNames = ["offer_id", "location_id"])]
)
data class OfferLocationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val offerId: Long = 0,
    @Column(nullable = false) val locationId: Long = 0
)
