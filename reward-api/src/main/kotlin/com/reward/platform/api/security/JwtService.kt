package com.reward.platform.api.security

import com.reward.platform.api.entity.SystemUserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

data class AuthClaims(
    val userId: Long,
    val email: String,
    val role: String,
    val tenantId: Long,
    val programId: Long,
    val sponsorId: Long?,
    val forcePasswordChange: Boolean
)

@Service
class JwtService(
    @Value("\${security.jwt.secret:change-me-in-prod-at-least-32-chars}") rawSecret: String,
    @Value("\${security.jwt.issuer:benevo-dashboard}") private val issuer: String,
    @Value("\${security.jwt.access-token-ttl-seconds:7200}") private val accessTokenTtlSeconds: Long
) {
    private val signingKey = Keys.hmacShaKeyFor(rawSecret.toByteArray(StandardCharsets.UTF_8).copyOf(32))

    fun createAccessToken(user: SystemUserEntity): Pair<String, Long> {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(accessTokenTtlSeconds)

        val token = Jwts.builder()
            .subject(user.id.toString())
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim("identityType", "SYSTEM_USER")
            .claim("email", user.email)
            .claim("role", user.role)
            .claim("tenantId", user.tenantId)
            .claim("programId", user.programId)
            .claim("sponsorId", user.sponsorId)
            .claim("forcePasswordChange", user.forcePasswordChange)
            .signWith(signingKey)
            .compact()

        return token to accessTokenTtlSeconds
    }

    fun parseAccessToken(token: String): AuthClaims {
        val claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload

        require(claims.issuer == issuer) { "Invalid token issuer" }

        val userId = claims.subject?.toLongOrNull() ?: error("Token subject is missing")
        val email = claims["email"]?.toString() ?: error("Token email is missing")
        val role = claims["role"]?.toString() ?: error("Token role is missing")
        val tenantId = claims["tenantId"].toLongClaim()
        val programId = claims["programId"].toLongClaim()
        val sponsorId = claims["sponsorId"].toLongClaimOrNull()
        val forcePasswordChange = claims["forcePasswordChange"].toBooleanClaim()

        return AuthClaims(
            userId = userId,
            email = email,
            role = role,
            tenantId = tenantId,
            programId = programId,
            sponsorId = sponsorId,
            forcePasswordChange = forcePasswordChange
        )
    }

    private fun Any?.toLongClaim(): Long {
        return when (this) {
            is Number -> this.toLong()
            is String -> this.toLongOrNull() ?: error("Invalid numeric claim")
            else -> error("Missing numeric claim")
        }
    }

    private fun Any?.toLongClaimOrNull(): Long? {
        return when (this) {
            null -> null
            is Number -> this.toLong()
            is String -> this.toLongOrNull()
            else -> null
        }
    }

    private fun Any?.toBooleanClaim(): Boolean {
        return when (this) {
            is Boolean -> this
            is String -> this.toBooleanStrictOrNull() ?: false
            else -> false
        }
    }
}