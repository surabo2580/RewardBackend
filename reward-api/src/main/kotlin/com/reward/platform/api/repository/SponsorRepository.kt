package com.reward.platform.api.repository

import com.reward.platform.api.entity.SponsorEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SponsorRepository : JpaRepository<SponsorEntity, Long> {
    fun findByTenantIdAndProgramIdOrderByName(tenantId: Long, programId: Long): List<SponsorEntity>
    fun findByTenantIdAndProgramIdAndSponsorCode(tenantId: Long, programId: Long, sponsorCode: String): SponsorEntity?
}