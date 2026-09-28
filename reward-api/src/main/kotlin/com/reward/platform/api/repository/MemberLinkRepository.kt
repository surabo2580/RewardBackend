package com.reward.platform.api.repository

import com.reward.platform.api.entity.MemberLinkEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberLinkRepository : JpaRepository<MemberLinkEntity, Long> {
    @Query("""
        select l from MemberLinkEntity l
        where l.tenantId = :tenantId and (l.primaryMemberId = :memberId or l.linkedMemberId = :memberId)
        order by l.createdAt desc
    """)
    fun findAllForMember(@Param("tenantId") tenantId: Long, @Param("memberId") memberId: Long): List<MemberLinkEntity>
    fun findByTenantIdAndId(tenantId: Long, id: Long): MemberLinkEntity?
    fun existsByTenantIdAndPrimaryMemberIdAndLinkedMemberId(tenantId: Long, primaryMemberId: Long, linkedMemberId: Long): Boolean
}
