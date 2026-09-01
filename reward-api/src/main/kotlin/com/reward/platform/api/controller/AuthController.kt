package com.reward.platform.api.controller

import com.reward.platform.api.dto.ChangePasswordRequest
import com.reward.platform.api.dto.ForgotPasswordRequest
import com.reward.platform.api.dto.ForgotPasswordResponse
import com.reward.platform.api.dto.LoginRequest
import com.reward.platform.api.dto.LoginResponse
import com.reward.platform.api.dto.ResetPasswordRequest
import com.reward.platform.api.entity.PasswordResetTokenEntity
import com.reward.platform.api.repository.PasswordResetTokenRepository
import com.reward.platform.api.dto.SystemUserProfileResponse
import com.reward.platform.api.repository.SystemUserRepository
import com.reward.platform.api.security.ApiKeyService
import com.reward.platform.api.security.JwtService
import com.reward.platform.api.security.TenantHostResolver
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val systemUserRepository: SystemUserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val jwtService: JwtService,
    private val tenantHostResolver: TenantHostResolver,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${security.password-reset-ttl-minutes:30}") private val passwordResetTtlMinutes: Long
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<LoginResponse> {
        val identifier = request.identifier.trim()
        require(identifier.isNotBlank()) { "Email or username is required" }

        val user = if (identifier.contains("@")) {
            systemUserRepository.findByEmailIgnoreCase(identifier)
        } else {
            systemUserRepository.findByUsernameIgnoreCase(identifier)
                ?: systemUserRepository.findByEmailIgnoreCase(identifier)
        }

        require(user != null) { "Invalid credentials" }
        require(user.status == "ACTIVE") { "User is not active" }
        require(passwordEncoder.matches(request.password, user.passwordHash)) { "Invalid credentials" }

        val hostTenantId = tenantHostResolver.resolveTenantId(httpRequest.getHeader("Host") ?: httpRequest.serverName)
        if (hostTenantId != null) {
            require(hostTenantId == user.tenantId) { "User does not belong to this tenant host" }
        }

        val (accessToken, expiresInSeconds) = jwtService.createAccessToken(user)
        systemUserRepository.save(user.copy(lastLoginAt = Instant.now()))

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                expiresInSeconds = expiresInSeconds,
                user = SystemUserProfileResponse.from(user),
                mustChangePassword = user.forcePasswordChange
            )
        )
    }

    @PostMapping("/change-password")
    fun changePassword(
        @RequestAttribute("authUserId") authUserId: Long,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<LoginResponse> {
        val user = systemUserRepository.findById(authUserId).orElse(null)
        require(user != null && user.status == "ACTIVE") { "User is not authenticated" }
        require(passwordEncoder.matches(request.currentPassword, user.passwordHash)) { "Current password is invalid" }
        require(request.currentPassword != request.newPassword) { "New password must be different" }

        val updatedUser = systemUserRepository.save(
            user.copy(
                passwordHash = passwordEncoder.encode(request.newPassword) ?: error("Password encoding failed"),
                forcePasswordChange = false,
                lastLoginAt = Instant.now()
            )
        )

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

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<ForgotPasswordResponse> {
        val normalizedEmail = request.email.trim().lowercase()
        val user = systemUserRepository.findByEmailIgnoreCase(normalizedEmail)
        if (user == null || (user.status != "ACTIVE" && user.status != "INVITED")) {
            return ResponseEntity.ok(
                ForgotPasswordResponse(message = "If the account exists, password reset instructions have been generated.")
            )
        }

        val rawToken = ApiKeyService.generate()
        val tokenHash = ApiKeyService.hash(rawToken)
        passwordResetTokenRepository.save(
            PasswordResetTokenEntity(
                userId = user.id,
                tokenHash = tokenHash,
                expiresAt = Instant.now().plusSeconds(passwordResetTtlMinutes * 60)
            )
        )

        return ResponseEntity.ok(
            ForgotPasswordResponse(
                message = "Password reset token generated. Use this token on reset password screen.",
                resetToken = rawToken
            )
        )
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<LoginResponse> {
        val tokenHash = ApiKeyService.hash(request.token.trim())
        val tokenEntity = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
        require(tokenEntity != null) { "Invalid or used password reset token" }
        require(tokenEntity.expiresAt.isAfter(Instant.now())) { "Password reset token expired" }

        val user = systemUserRepository.findById(tokenEntity.userId).orElse(null)
        require(user != null) { "Associated user not found" }

        val updatedUser = systemUserRepository.save(
            user.copy(
                passwordHash = passwordEncoder.encode(request.newPassword) ?: error("Password encoding failed"),
                status = "ACTIVE",
                forcePasswordChange = false,
                lastLoginAt = Instant.now()
            )
        )
        passwordResetTokenRepository.save(tokenEntity.copy(usedAt = Instant.now()))

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

    @GetMapping("/me")
    fun me(@RequestAttribute("authUserId") authUserId: Long): ResponseEntity<SystemUserProfileResponse> {
        val user = systemUserRepository.findById(authUserId).orElse(null)
        require(user != null && user.status == "ACTIVE") { "User is not authenticated" }
        return ResponseEntity.ok(SystemUserProfileResponse.from(user))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "Logged out"))
    }
}