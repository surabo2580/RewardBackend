package com.reward.platform.api.repository

import com.reward.platform.api.entity.MemberEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param

interface MemberRepository : JpaRepository<MemberEntity, Long> {
    fun findByTenantIdAndExternalUserId(tenantId: Long, externalUserId: String): MemberEntity?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLockedByIdAndTenantId(id: Long, tenantId: Long): MemberEntity?
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: Long): List<MemberEntity>
    fun findByIdAndTenantId(id: Long, tenantId: Long): MemberEntity?
    fun findFirstByTenantIdAndEmailIgnoreCase(tenantId: Long, email: String): MemberEntity?

    @Query("""
        select m from MemberEntity m
        where m.tenantId = :tenantId and (
            lower(m.email) like lower(concat('%', :query, '%'))
            or lower(m.externalUserId) like lower(concat('%', :query, '%'))
            or m.phone like concat('%', :query, '%')
            or lower(concat(coalesce(m.firstName, ''), ' ', coalesce(m.lastName, ''))) like lower(concat('%', :query, '%'))
        )
        order by m.createdAt desc
    """)
    fun search(@Param("tenantId") tenantId: Long, @Param("query") query: String, pageable: Pageable): List<MemberEntity>
}
