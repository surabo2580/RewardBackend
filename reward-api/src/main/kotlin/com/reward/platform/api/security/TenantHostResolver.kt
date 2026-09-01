package com.reward.platform.api.security

import com.reward.platform.api.repository.TenantRepository
import org.springframework.stereotype.Component

@Component
class TenantHostResolver(
    private val tenantRepository: TenantRepository
) {
    fun resolveTenantId(hostHeader: String?): Long? {
        val normalizedHost = hostHeader
            ?.substringBefore(':')
            ?.trim()
            ?.lowercase()
            ?: return null

        if (normalizedHost == "localhost" || normalizedHost.endsWith(".localhost")) {
            return null
        }

        val slug = normalizedHost.substringBefore('.')
        if (slug.isBlank() || slug == normalizedHost) {
            return null
        }

        return tenantRepository.findBySlug(slug)?.id
    }
}