package com.reward.platform.api.repository

import com.reward.platform.api.entity.OfferTargetMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OfferTargetMemberRepository : JpaRepository<OfferTargetMemberEntity, Long> {
    fun existsByOfferIdAndMemberId(offerId: Long, memberId: Long): Boolean
}