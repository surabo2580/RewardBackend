package com.reward.platform.api.repository

import com.reward.platform.api.entity.MembershipCardEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MembershipCardRepository : JpaRepository<MembershipCardEntity, Long> {
    fun findByTenantIdAndMemberIdOrderByIssuedAtDesc(tenantId: Long, memberId: Long): List<MembershipCardEntity>
    fun findByTenantIdAndMemberIdAndId(tenantId: Long, memberId: Long, id: Long): MembershipCardEntity?
    fun existsByTenantIdAndCardNumber(tenantId: Long, cardNumber: String): Boolean
}
