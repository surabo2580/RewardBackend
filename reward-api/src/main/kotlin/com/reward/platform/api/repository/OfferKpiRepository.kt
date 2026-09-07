package com.reward.platform.api.repository

import com.reward.platform.api.entity.OfferKpiEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OfferKpiRepository : JpaRepository<OfferKpiEntity, Long> {
    fun findByOfferIdIn(offerIds: Collection<Long>): List<OfferKpiEntity>
    fun findByOfferId(offerId: Long): List<OfferKpiEntity>
    fun deleteByOfferId(offerId: Long)
}
