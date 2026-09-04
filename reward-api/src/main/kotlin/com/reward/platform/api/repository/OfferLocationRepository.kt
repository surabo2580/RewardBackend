package com.reward.platform.api.repository

import com.reward.platform.api.entity.OfferLocationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OfferLocationRepository : JpaRepository<OfferLocationEntity, Long> {
    fun findByOfferIdIn(offerIds: Collection<Long>): List<OfferLocationEntity>
    fun findByOfferId(offerId: Long): List<OfferLocationEntity>
    fun existsByOfferIdAndLocationId(offerId: Long, locationId: Long): Boolean
    fun deleteByOfferId(offerId: Long)
}
