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
import org.springframework.web.bind.annotation.RequestAttribute

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/branches")
class BranchController(
    private val branchRepository: BranchRepository
) {

    @PostMapping
    fun createBranch(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: BranchCreateRequest
    ): ResponseEntity<BranchResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        require(!branchRepository.existsByTenantIdAndCode(request.tenantId, request.code)) {
            "Branch code already exists for tenant"
        }
        request.parentBranchId?.let { parentId ->
            val parent = branchRepository.findById(parentId).orElse(null)
            require(parent != null && parent.tenantId == request.tenantId) {
                "Parent branch does not belong to tenant"
            }
        }
        val branch = BranchEntity(
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
    fun listBranches(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<List<BranchResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        return ResponseEntity.ok(
            branchRepository.findByTenantIdOrderByName(tenantId).map(BranchResponse::from)
        )
    }
}
