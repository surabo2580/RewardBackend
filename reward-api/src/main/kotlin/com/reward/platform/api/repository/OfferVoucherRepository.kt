package com.reward.platform.api.repository

import com.reward.platform.api.entity.OfferVoucherEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface OfferVoucherRepository : JpaRepository<OfferVoucherEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findFirstByTenantIdAndOfferIdAndIsIssuedFalse(tenantId: Long, offerId: Long): OfferVoucherEntity?
    fun findByTenantIdAndReferenceId(tenantId: Long, referenceId: String): OfferVoucherEntity?
}