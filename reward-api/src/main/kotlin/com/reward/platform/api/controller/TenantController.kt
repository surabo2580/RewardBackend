package com.reward.platform.api.controller

import com.reward.platform.api.entity.TenantEntity
import com.reward.platform.api.repository.TenantRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tenants")
class TenantController(
    private val tenantRepository: TenantRepository
) {

    @PostMapping
    fun createTenant(@RequestBody tenant: TenantEntity): ResponseEntity<TenantEntity> {
        return ResponseEntity.ok(tenantRepository.save(tenant))
    }

    @GetMapping
    fun listTenants(): ResponseEntity<List<TenantEntity>> {
        return ResponseEntity.ok(tenantRepository.findAll())
    }
}
