package com.reward.platform.api.dto

import com.reward.platform.api.entity.SystemUserEntity
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val identifier: String = "",

    @field:NotBlank
    val password: String = ""
)

data class SystemUserProfileResponse(
    val userId: Long,
    val email: String,
    val username: String,
    val role: String,
    val tenantId: Long,
    val programId: Long,
    val sponsorId: Long?
) {
    companion object {
        fun from(entity: SystemUserEntity): SystemUserProfileResponse {
            return SystemUserProfileResponse(
                userId = entity.id,
                email = entity.email,
                username = entity.username,
                role = entity.role,
                tenantId = entity.tenantId,
                programId = entity.programId,
                sponsorId = entity.sponsorId
            )
        }
    }
}

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: SystemUserProfileResponse
)