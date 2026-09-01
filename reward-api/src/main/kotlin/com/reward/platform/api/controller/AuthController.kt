package com.reward.platform.api.controller

import com.reward.platform.api.dto.LoginRequest
import com.reward.platform.api.dto.LoginResponse
import com.reward.platform.api.dto.SystemUserProfileResponse
import com.reward.platform.api.repository.SystemUserRepository
import com.reward.platform.api.security.JwtService
import com.reward.platform.api.security.TenantHostResolver
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
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
    private val jwtService: JwtService,
    private val tenantHostResolver: TenantHostResolver,
    private val passwordEncoder: PasswordEncoder
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
                user = SystemUserProfileResponse.from(user)
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