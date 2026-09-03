package com.reward.platform.api.repository

import com.reward.platform.api.entity.OfferEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface OfferRepository : JpaRepository<OfferEntity, Long> {
    fun findByTenantIdAndProgramIdOrderByCreatedAtDesc(tenantId: Long, programId: Long): List<OfferEntity>
    fun findByTenantIdAndProgramIdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        tenantId: Long,
        programId: Long,
        startDate: Instant,
        endDate: Instant
    ): List<OfferEntity>
}