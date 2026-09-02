package com.reward.platform.api.repository

import com.reward.platform.api.entity.BranchEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BranchRepository : JpaRepository<BranchEntity, Long> {
    fun findByTenantIdAndCode(tenantId: Long, code: String): BranchEntity?
    fun findByTenantIdAndCodeAndStatus(tenantId: Long, code: String, status: String): BranchEntity?
    fun findByTenantIdOrderByName(tenantId: Long): List<BranchEntity>
    fun existsByTenantIdAndCode(tenantId: Long, code: String): Boolean
}
