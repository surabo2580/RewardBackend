package com.reward.platform.api.controller

import com.reward.platform.api.entity.ProgramEntity
import com.reward.platform.api.repository.ProgramRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/programs")
class ProgramController(
    private val programRepository: ProgramRepository
) {

    @PostMapping
    fun createProgram(@RequestBody program: ProgramEntity): ResponseEntity<ProgramEntity> {
        return ResponseEntity.ok(programRepository.save(program))
    }

    @GetMapping("/{tenantId}")
    fun getProgramsByTenant(@PathVariable tenantId: String): ResponseEntity<List<ProgramEntity>> {
        return ResponseEntity.ok(programRepository.findAll().filter { it.tenantId == tenantId })
    }
}
