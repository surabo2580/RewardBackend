package com.reward.platform.api.repository

import com.reward.platform.api.entity.OfferApplicationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OfferApplicationRepository : JpaRepository<OfferApplicationEntity, Long> {
    fun countByTenantIdAndMemberIdAndOfferId(tenantId: Long, memberId: Long, offerId: Long): Long
}