package com.reward.platform.api.dto

import com.reward.platform.api.entity.BranchEntity
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class BranchCreateRequest(
    @field:NotBlank(message = "Tenant id is required")
    val tenantId: String = "",

    val parentBranchId: String? = null,

    @field:NotBlank(message = "Branch code is required")
    val code: String = "",

    @field:NotBlank(message = "Branch name is required")
    val name: String = "",

    val city: String? = null,
    val status: String = "ACTIVE"
)

data class BranchResponse(
    val id: String,
    val tenantId: String,
    val parentBranchId: String?,
    val code: String,
    val name: String,
    val city: String?,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: BranchEntity) = BranchResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            parentBranchId = entity.parentBranchId,
            code = entity.code,
            name = entity.name,
            city = entity.city,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}
