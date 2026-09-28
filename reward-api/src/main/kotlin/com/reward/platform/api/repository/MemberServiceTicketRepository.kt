package com.reward.platform.api.repository

import com.reward.platform.api.entity.MemberServiceTicketEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberServiceTicketRepository : JpaRepository<MemberServiceTicketEntity, Long> {
    fun findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId: Long, memberId: Long): List<MemberServiceTicketEntity>
    fun findByTenantIdAndMemberIdAndId(tenantId: Long, memberId: Long, id: Long): MemberServiceTicketEntity?
    fun findTop20ByTenantIdAndIsHotnoteTrueAndStatusInOrderByCreatedAtDesc(tenantId: Long, statuses: Collection<String>): List<MemberServiceTicketEntity>
}
