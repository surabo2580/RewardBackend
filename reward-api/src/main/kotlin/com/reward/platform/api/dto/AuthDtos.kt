package com.reward.platform.api.dto

import com.reward.platform.api.entity.SystemUserEntity
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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
    val user: SystemUserProfileResponse,
    val mustChangePassword: Boolean = false
)

data class ChangePasswordRequest(
    @field:NotBlank
    val currentPassword: String = "",

    @field:Size(min = 8, max = 72)
    val newPassword: String = ""
)

data class ForgotPasswordRequest(
    @field:Email
    @field:NotBlank
    val email: String = ""
)

data class ForgotPasswordResponse(
    val message: String,
    val resetToken: String? = null
)

data class ResetPasswordRequest(
    @field:NotBlank
    val token: String = "",

    @field:Size(min = 8, max = 72)
    val newPassword: String = ""
)