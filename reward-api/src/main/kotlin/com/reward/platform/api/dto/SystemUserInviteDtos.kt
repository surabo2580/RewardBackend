package com.reward.platform.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class InviteSystemUserRequest(
    @field:Email
    @field:NotBlank
    val email: String = "",

    @field:NotBlank
    val role: String = "PROGRAM_ANALYST",

    val sponsorId: Long? = null
)

data class InviteSystemUserResponse(
    val userId: Long,
    val email: String,
    val role: String,
    val status: String,
    val expiresAt: Instant,
    val inviteToken: String,
    val inviteLink: String
)

data class AcceptInviteRequest(
    @field:NotBlank
    val token: String = "",

    @field:Size(min = 8, max = 72)
    val password: String = ""
)