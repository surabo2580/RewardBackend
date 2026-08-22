package com.reward.platform.api.repository

import com.reward.platform.api.entity.BranchEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BranchRepository : JpaRepository<BranchEntity, String> {
    fun findByTenantIdAndCode(tenantId: String, code: String): BranchEntity?
    fun findByTenantIdOrderByName(tenantId: String): List<BranchEntity>
}
