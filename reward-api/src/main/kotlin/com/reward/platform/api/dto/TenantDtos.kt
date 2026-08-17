package com.reward.platform.api.dto

import com.reward.platform.api.entity.TenantEntity
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class TenantCreateRequest(
    @field:NotBlank(message = "Tenant name is required")
    val name: String = "",

    val status: String = "ACTIVE"
)

data class TenantResponse(
    val id: String,
    val name: String,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: TenantEntity): TenantResponse = TenantResponse(
            id = entity.id,
            name = entity.name,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}
