package com.reward.platform.api.controller

import com.reward.platform.api.dto.ProgramResponse
import com.reward.platform.api.dto.SponsorResponse
import com.reward.platform.api.dto.TenantProvisionRequest
import com.reward.platform.api.dto.TenantProvisionResponse
import com.reward.platform.api.dto.SystemUserBootstrapCredentials
import com.reward.platform.api.dto.TierResponse
import com.reward.platform.api.dto.TenantResponse
import com.reward.platform.api.service.TenantProvisioningService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/provisioning")
class ProvisioningController(
    private val tenantProvisioningService: TenantProvisioningService
) {

    @PostMapping("/tenants")
    fun provisionTenant(
        @Valid @RequestBody request: TenantProvisionRequest
    ): ResponseEntity<TenantProvisionResponse> {
        val provisioned = tenantProvisioningService.provisionTenant(request)

        return ResponseEntity.ok(
            TenantProvisionResponse(
                tenant = TenantResponse.from(provisioned.tenant),
                program = ProgramResponse.from(provisioned.program),
                hostSponsor = SponsorResponse.from(provisioned.hostSponsor),
                tiers = provisioned.tiers.map {
                    TierResponse(
                        id = it.id,
                        tenantId = it.tenantId,
                        programId = it.programId,
                        name = it.name,
                        rank = it.rank,
                        thresholdPoints = it.thresholdPoints,
                        multiplier = it.multiplier,
                        createdAt = it.createdAt
                    )
                },
                apiKey = provisioned.apiKey,
                systemUser = SystemUserBootstrapCredentials(
                    email = provisioned.systemUser.email,
                    username = provisioned.systemUser.username,
                    temporaryPassword = provisioned.rawPassword,
                    role = provisioned.systemUser.role
                )
            )
        )
    }
}
