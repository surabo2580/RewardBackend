package com.reward.platform.api.repository

import com.reward.platform.api.entity.BitEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface BitRepository : JpaRepository<BitEntity, Long>, JpaSpecificationExecutor<BitEntity> {
    fun findByTenantIdAndBitReferenceAndBitType(tenantId: Long, bitReference: String, bitType: String): BitEntity?
    fun findByTenantIdAndId(tenantId: Long, id: Long): BitEntity?
    fun findByTenantIdAndMemberIdOrderByInteractionAtDesc(tenantId: Long, memberId: Long): List<BitEntity>
    fun findByTenantIdAndMemberIdOrderByInteractionAtDesc(tenantId: Long, memberId: Long, pageable: Pageable): List<BitEntity>
    fun findByTenantIdAndOriginalBitId(tenantId: Long, originalBitId: Long): List<BitEntity>
}
