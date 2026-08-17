package com.reward.platform.api.controller

import com.reward.platform.api.dto.ProgramCreateRequest
import com.reward.platform.api.dto.ProgramResponse
import com.reward.platform.api.entity.ProgramEntity
import com.reward.platform.api.repository.ProgramRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/programs")
class ProgramController(
    private val programRepository: ProgramRepository
) {

    @PostMapping
    fun createProgram(@Valid @RequestBody request: ProgramCreateRequest): ResponseEntity<ProgramResponse> {
        val entity = ProgramEntity(
            id = UUID.randomUUID().toString(),
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
    fun getProgramsByTenant(@PathVariable tenantId: String): ResponseEntity<List<ProgramResponse>> {
        return ResponseEntity.ok(
            programRepository.findAll()
                .filter { it.tenantId == tenantId }
                .map(ProgramResponse::from)
        )
    }
}
