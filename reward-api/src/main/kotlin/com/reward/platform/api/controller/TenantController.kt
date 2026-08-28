package com.reward.platform.api.controller

import com.reward.platform.api.dto.TenantCreateRequest
import com.reward.platform.api.dto.TenantResponse
import com.reward.platform.api.entity.TenantEntity
import com.reward.platform.api.repository.TenantRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.beans.factory.annotation.Value
import com.reward.platform.api.security.ApiKeyService

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/tenants")
class TenantController(
    private val tenantRepository: TenantRepository,
    @Value("\${PLATFORM_BASE_DOMAIN:benevo.io}") private val baseDomain: String
) {

    @PostMapping
    fun createTenant(@Valid @RequestBody request: TenantCreateRequest): ResponseEntity<TenantResponse> {
        val entity = TenantEntity(
            name = request.name,
            slug = request.slug ?: request.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
            baseUrl = "https://${request.slug ?: request.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}.$baseDomain",
            schemaName = request.slug ?: request.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_'),
            apiKeyHash = ApiKeyService.hash(ApiKeyService.generate()),
            adminEmail = request.adminEmail,
            status = request.status
        )
        return ResponseEntity.ok(TenantResponse.from(tenantRepository.save(entity)))
    }

    @GetMapping
    fun listTenants(): ResponseEntity<List<TenantResponse>> {
        return ResponseEntity.ok(tenantRepository.findAll().map(TenantResponse::from))
    }
}
