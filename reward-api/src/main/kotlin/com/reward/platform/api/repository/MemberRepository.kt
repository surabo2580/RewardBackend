package com.reward.platform.api.repository

import com.reward.platform.api.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<MemberEntity, Long> {
    fun findByTenantIdAndExternalUserId(tenantId: Long, externalUserId: String): MemberEntity?
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: Long): List<MemberEntity>
}
