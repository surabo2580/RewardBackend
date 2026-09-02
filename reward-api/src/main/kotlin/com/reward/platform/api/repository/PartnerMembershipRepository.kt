package com.reward.platform.api.repository

import com.reward.platform.api.entity.PartnerMembershipEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PartnerMembershipRepository : JpaRepository<PartnerMembershipEntity, Long> {
    fun findByTenantIdAndSponsorIdAndExternalMembershipId(
        tenantId: Long,
        sponsorId: Long,
        externalMembershipId: String
    ): PartnerMembershipEntity?

    fun findByTenantIdAndSponsorIdOrderByCreatedAtDesc(tenantId: Long, sponsorId: Long): List<PartnerMembershipEntity>
}