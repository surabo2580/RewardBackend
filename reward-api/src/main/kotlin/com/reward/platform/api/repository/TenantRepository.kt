package com.reward.platform.api.repository

import com.reward.platform.api.entity.TenantEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TenantRepository : JpaRepository<TenantEntity, Long>
{
	fun findBySlug(slug: String): TenantEntity?
	fun findByApiKeyHash(apiKeyHash: String): TenantEntity?
	fun findByStatus(status: String): List<TenantEntity>
}
