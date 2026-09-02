package com.reward.platform.api.repository

import com.reward.platform.api.entity.SponsorLocationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SponsorLocationRepository : JpaRepository<SponsorLocationEntity, Long> {
    fun findByTenantIdAndSponsorIdOrderByLocationName(tenantId: Long, sponsorId: Long): List<SponsorLocationEntity>
    fun findByTenantIdAndLocationCode(tenantId: Long, locationCode: String): SponsorLocationEntity?
    fun findByTenantIdAndSponsorIdAndLocationCode(
        tenantId: Long,
        sponsorId: Long,
        locationCode: String
    ): SponsorLocationEntity?
}