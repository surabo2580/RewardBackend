package com.reward.platform.api.controller

import com.reward.platform.api.dto.AcceptInviteRequest
import com.reward.platform.api.dto.InviteSystemUserRequest
import com.reward.platform.api.dto.InviteSystemUserResponse
import com.reward.platform.api.dto.LoginResponse
import com.reward.platform.api.dto.SystemUserProfileResponse
import com.reward.platform.api.entity.SystemUserEntity
import com.reward.platform.api.entity.SystemUserInviteTokenEntity
import com.reward.platform.api.repository.SystemUserInviteTokenRepository
import com.reward.platform.api.repository.SystemUserRepository
import com.reward.platform.api.security.ApiKeyService
import com.reward.platform.api.security.JwtService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/users")
class SystemUserController(
    private val systemUserRepository: SystemUserRepository,
    private val inviteTokenRepository: SystemUserInviteTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    @Value("\${security.invite-token-ttl-hours:72}") private val inviteTtlHours: Long
) {

    @PostMapping("/invite")
    fun inviteSystemUser(
        @RequestAttribute("authUserId") authUserId: Long,
        @RequestAttribute("authIdentityType") authIdentityType: String,
        @RequestAttribute("authRole") authRole: String,
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute("programId") programId: Long,
        @Valid @RequestBody request: InviteSystemUserRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<InviteSystemUserResponse> {
        require(authIdentityType == "SYSTEM_USER") { "Only system users can invite team members" }
        require(authRole == "TENANT_ADMIN" || authRole == "PROGRAM_MANAGER") { "Insufficient privileges to invite users" }

        val normalizedEmail = request.email.trim().lowercase()
        val existingUser = systemUserRepository.findByEmailIgnoreCase(normalizedEmail)
        require(existingUser == null) { "A user with this email already exists" }

        val username = uniqueUsername(normalizedEmail.substringBefore('@').ifBlank { "user" })
        val temporaryPassword = buildString {
            append("Inv!")
            append(ApiKeyService.generate().replace("-", "a").replace("_", "B").take(10))
            append("7")
        }
        val passwordHash = passwordEncoder.encode(temporaryPassword) ?: error("Password encoding failed")

        val invitedUser = systemUserRepository.save(
            SystemUserEntity(
                email = normalizedEmail,
                username = username,
                passwordHash = passwordHash,
                tenantId = tenantId,
                programId = programId,
                sponsorId = request.sponsorId,
                role = request.role.trim().uppercase(),
                status = "INVITED",
                forcePasswordChange = true
            )
        )

        val rawToken = ApiKeyService.generate()
        val expiresAt = Instant.now().plusSeconds(inviteTtlHours * 3600)
        inviteTokenRepository.save(
            SystemUserInviteTokenEntity(
                userId = invitedUser.id,
                tokenHash = ApiKeyService.hash(rawToken),
                expiresAt = expiresAt
            )
        )

        val host = httpRequest.getHeader("Origin")?.removeSuffix("/")
            ?: "${httpRequest.scheme}://${httpRequest.serverName}:${httpRequest.serverPort}"
        val inviteLink = "$host/invite/accept?token=$rawToken"

        return ResponseEntity.ok(
            InviteSystemUserResponse(
                userId = invitedUser.id,
                email = invitedUser.email,
                role = invitedUser.role,
                status = invitedUser.status,
                expiresAt = expiresAt,
                inviteToken = rawToken,
                inviteLink = inviteLink
            )
        )
    }

    @PostMapping("/accept-invite")
    fun acceptInvite(@Valid @RequestBody request: AcceptInviteRequest): ResponseEntity<LoginResponse> {
        val tokenHash = ApiKeyService.hash(request.token.trim())
        val inviteToken = inviteTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
        require(inviteToken != null) { "Invalid or used invite token" }
        require(inviteToken.expiresAt.isAfter(Instant.now())) { "Invite token expired" }

        val user = systemUserRepository.findById(inviteToken.userId).orElse(null)
        require(user != null) { "Invited user not found" }

        val updatedUser = systemUserRepository.save(
            user.copy(
                passwordHash = passwordEncoder.encode(request.password) ?: error("Password encoding failed"),
                status = "ACTIVE",
                forcePasswordChange = false,
                lastLoginAt = Instant.now()
            )
        )
        inviteTokenRepository.save(inviteToken.copy(usedAt = Instant.now()))

        val (accessToken, expiresInSeconds) = jwtService.createAccessToken(updatedUser)
        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                expiresInSeconds = expiresInSeconds,
                user = SystemUserProfileResponse.from(updatedUser),
                mustChangePassword = false
            )
        )
    }

    private fun uniqueUsername(base: String): String {
        val normalizedBase = base.lowercase().replace(Regex("[^a-z0-9._-]"), "").ifBlank { "user" }
        var candidate = normalizedBase
        var index = 1
        while (systemUserRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = "${normalizedBase}_$index"
            index += 1
        }
        return candidate
    }
}