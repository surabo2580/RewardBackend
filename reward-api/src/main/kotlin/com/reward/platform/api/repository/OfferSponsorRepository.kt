package com.reward.platform.api.repository

import com.reward.platform.api.entity.OfferSponsorEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OfferSponsorRepository : JpaRepository<OfferSponsorEntity, Long> {
    fun findByOfferIdIn(offerIds: Collection<Long>): List<OfferSponsorEntity>
    fun findByOfferId(offerId: Long): List<OfferSponsorEntity>
    fun deleteByOfferId(offerId: Long)
}