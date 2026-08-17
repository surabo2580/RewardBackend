package com.reward.platform.api.dto

import com.reward.platform.api.entity.MemberEntity
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class MemberCreateRequest(
    @field:NotBlank(message = "Tenant id is required")
    val tenantId: String = "",

    @field:NotBlank(message = "External user id is required")
    val externalUserId: String = "",

    val email: String? = null,
    val tier: String = "STANDARD"
)

data class MemberResponse(
    val id: String,
    val tenantId: String,
    val externalUserId: String,
    val email: String?,
    val tier: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: MemberEntity): MemberResponse = MemberResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            externalUserId = entity.externalUserId,
            email = entity.email,
            tier = entity.tier,
            createdAt = entity.createdAt
        )
    }
}
