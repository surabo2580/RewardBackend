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
import java.util.UUID
import org.springframework.web.bind.annotation.CrossOrigin

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/tenants")
class TenantController(
    private val tenantRepository: TenantRepository
) {

    @PostMapping
    fun createTenant(@Valid @RequestBody request: TenantCreateRequest): ResponseEntity<TenantResponse> {
        val entity = TenantEntity(
            id = UUID.randomUUID().toString(),
            name = request.name,
            status = request.status
        )
        return ResponseEntity.ok(TenantResponse.from(tenantRepository.save(entity)))
    }

    @GetMapping
    fun listTenants(): ResponseEntity<List<TenantResponse>> {
        return ResponseEntity.ok(tenantRepository.findAll().map(TenantResponse::from))
    }
}
