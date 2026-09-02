package com.reward.platform.api.security

import com.reward.platform.api.repository.SystemUserRepository
import com.reward.platform.api.repository.TenantRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiKeyFilter(
    private val tenantRepository: TenantRepository,
    private val systemUserRepository: SystemUserRepository,
    private val jwtService: JwtService,
    private val tenantHostResolver: TenantHostResolver
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.method == "OPTIONS" ||
            (request.method == "POST" && request.requestURI == "/api/provisioning/tenants") ||
            (request.method == "POST" && request.requestURI == "/api/auth/login") ||
            (request.method == "POST" && request.requestURI == "/api/auth/forgot-password") ||
            (request.method == "POST" && request.requestURI == "/api/auth/reset-password") ||
            (request.method == "POST" && request.requestURI == "/api/users/accept-invite") ||
            (request.method == "POST" && request.requestURI == "/api/public/register") ||
            (request.method == "POST" && request.requestURI == "/api/public/enterprise-inquiries") ||
            (request.method == "POST" && request.requestURI == "/api/admin/provisioning/enterprise") ||
            request.requestURI == "/api/health"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authorization = request.getHeader("Authorization")
        if (authorization?.startsWith("Bearer ") == true) {
            val token = authorization.removePrefix("Bearer ").trim()
            val claims = try {
                jwtService.parseAccessToken(token)
            } catch (_: Exception) {
                writeUnauthorized(request, response, "Valid Bearer token is required")
                return
            }

            val user = systemUserRepository.findById(claims.userId).orElse(null)
            if (user == null || user.status != "ACTIVE") {
                writeUnauthorized(request, response, "Authenticated user is not active")
                return
            }

            val tenantFromHost = tenantHostResolver.resolveTenantId(request.getHeader("Host") ?: request.serverName)
            if (tenantFromHost != null && tenantFromHost != claims.tenantId) {
                writeForbidden(request, response, "Tenant host does not match token tenant")
                return
            }

            request.setAttribute("tenantId", claims.tenantId)
            request.setAttribute("programId", claims.programId)
            request.setAttribute("sponsorId", claims.sponsorId)
            request.setAttribute("authUserId", claims.userId)
            request.setAttribute("authRole", claims.role)
            request.setAttribute("authIdentityType", "SYSTEM_USER")

            val firstLoginAllowed = request.requestURI == "/api/auth/change-password" ||
                request.requestURI == "/api/auth/logout" ||
                request.requestURI == "/api/auth/me"
            if (claims.forcePasswordChange && !firstLoginAllowed) {
                writeForbidden(request, response, "Password change required before accessing dashboard APIs")
                return
            }

            filterChain.doFilter(request, response)
            return
        }

        val apiKey = request.getHeader("X-API-Key")
        val tenant = apiKey?.let { tenantRepository.findByApiKeyHash(ApiKeyService.hash(it)) }

        if (tenant == null || tenant.status != "ACTIVE") {
            writeUnauthorized(request, response, "Valid X-API-Key header or Bearer token is required")
            return
        }

        request.setAttribute("tenantId", tenant.id)
        request.setAttribute("authIdentityType", "INTEGRATION_USER")
        filterChain.doFilter(request, response)
    }

    private fun writeUnauthorized(request: HttpServletRequest, response: HttpServletResponse, message: String) {
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") ?: "*")
        response.setHeader("Vary", "Origin")
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"error\":\"$message\"}")
    }

    private fun writeForbidden(request: HttpServletRequest, response: HttpServletResponse, message: String) {
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") ?: "*")
        response.setHeader("Vary", "Origin")
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"error\":\"$message\"}")
    }
}
