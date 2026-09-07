package com.reward.platform.api.repository

import com.reward.platform.api.entity.MemberEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface MemberRepository : JpaRepository<MemberEntity, Long> {
    fun findByTenantIdAndExternalUserId(tenantId: Long, externalUserId: String): MemberEntity?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLockedByIdAndTenantId(id: Long, tenantId: Long): MemberEntity?
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: Long): List<MemberEntity>
}
