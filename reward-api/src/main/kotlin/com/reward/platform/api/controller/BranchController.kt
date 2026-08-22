package com.reward.platform.api.controller

import com.reward.platform.api.dto.BranchCreateRequest
import com.reward.platform.api.dto.BranchResponse
import com.reward.platform.api.entity.BranchEntity
import com.reward.platform.api.repository.BranchRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/branches")
class BranchController(
    private val branchRepository: BranchRepository
) {

    @PostMapping
    fun createBranch(@Valid @RequestBody request: BranchCreateRequest): ResponseEntity<BranchResponse> {
        val branch = BranchEntity(
            id = UUID.randomUUID().toString(),
            tenantId = request.tenantId,
            parentBranchId = request.parentBranchId,
            code = request.code,
            name = request.name,
            city = request.city,
            status = request.status
        )
        return ResponseEntity.ok(BranchResponse.from(branchRepository.save(branch)))
    }

    @GetMapping
    fun listBranches(@RequestParam tenantId: String): ResponseEntity<List<BranchResponse>> {
        return ResponseEntity.ok(
            branchRepository.findByTenantIdOrderByName(tenantId).map(BranchResponse::from)
        )
    }
}
