package com.reward.platform.api.repository

import com.reward.platform.api.entity.MemberBookingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberBookingRepository : JpaRepository<MemberBookingEntity, Long> {
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: Long, memberId: Long): List<MemberBookingEntity>
    fun findByTenantIdAndMemberIdAndId(tenantId: Long, memberId: Long, id: Long): MemberBookingEntity?
    fun existsByTenantIdAndBookingReference(tenantId: Long, bookingReference: String): Boolean
}
