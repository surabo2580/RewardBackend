package com.reward.platform.api.security

import com.reward.platform.api.repository.TenantRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApiKeyFilter(
    private val tenantRepository: TenantRepository
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.method == "OPTIONS" ||
            (request.method == "POST" && request.requestURI == "/api/provisioning/tenants") ||
            request.requestURI == "/api/health"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val apiKey = request.getHeader("X-API-Key")
        val tenant = apiKey?.let { tenantRepository.findByApiKeyHash(ApiKeyService.hash(it)) }

        if (tenant == null || tenant.status != "ACTIVE") {
            // Filters run before Spring MVC's @CrossOrigin handling, so add CORS headers
            // manually here or the browser reports a misleading CORS error instead of the 401.
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin") ?: "*")
            response.setHeader("Vary", "Origin")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("{\"error\":\"Valid X-API-Key header is required\"}")
            return
        }

        request.setAttribute("tenantId", tenant.id)
        filterChain.doFilter(request, response)
    }
}
