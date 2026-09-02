package com.reward.platform.api.controller

import com.reward.platform.api.dto.ProgramCreateRequest
import com.reward.platform.api.dto.ProgramResponse
import com.reward.platform.api.entity.ProgramEntity
import com.reward.platform.api.repository.ProgramRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestAttribute

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/programs")
class ProgramController(
    private val programRepository: ProgramRepository
) {

    @PostMapping
    fun createProgram(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: ProgramCreateRequest
    ): ResponseEntity<ProgramResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        val entity = ProgramEntity(
            id = 0,
            tenantId = request.tenantId,
            name = request.name,
            currency = request.currency,
            timezone = request.timezone,
            status = request.status,
            earningRate = request.earningRate,
            redemptionRate = request.redemptionRate
        )
        return ResponseEntity.ok(ProgramResponse.from(programRepository.save(entity)))
    }

    @GetMapping("/{tenantId}")
    fun getProgramsByTenant(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @PathVariable tenantId: Long
    ): ResponseEntity<List<ProgramResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        return ResponseEntity.ok(
            programRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .map(ProgramResponse::from)
        )
    }
}