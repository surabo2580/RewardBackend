package com.reward.platform.api.repository

import com.reward.platform.api.entity.MemberImportJobEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberImportJobRepository : JpaRepository<MemberImportJobEntity, Long> {
    fun findByTenantIdAndId(tenantId: Long, id: Long): MemberImportJobEntity?
    fun findTop20ByTenantIdOrderByCreatedAtDesc(tenantId: Long): List<MemberImportJobEntity>
}
