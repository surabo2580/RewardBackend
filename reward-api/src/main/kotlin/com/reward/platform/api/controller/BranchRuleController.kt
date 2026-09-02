package com.reward.platform.api.controller

import com.reward.platform.api.dto.BranchRuleCreateRequest
import com.reward.platform.api.dto.BranchRuleResponse
import com.reward.platform.api.entity.BranchRuleEntity
import com.reward.platform.api.repository.BranchRuleRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/rules")
class BranchRuleController(
    private val branchRuleRepository: BranchRuleRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository
) {

    @PostMapping
    fun createRule(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: BranchRuleCreateRequest
    ): ResponseEntity<BranchRuleResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        val scope = request.scope.uppercase()
        require(scope in setOf("PROGRAM", "SPONSOR", "LOCATION", "PARENT", "PARTNER")) {
            "Rule scope must be PROGRAM, SPONSOR, LOCATION, PARENT, or PARTNER"
        }
        require(programRepository.findById(request.programId).orElse(null)?.tenantId == request.tenantId) {
            "Program does not belong to tenant"
        }
        if (scope == "SPONSOR" || scope == "LOCATION" || scope == "PARENT" || scope == "PARTNER") {
            val sponsor = request.sponsorId?.let { sponsorRepository.findById(it).orElse(null) }
            require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) {
                "Sponsor does not belong to tenant and program"
            }
            if (scope == "PARENT") {
                require(sponsor.sponsorType == "HOST") { "PARENT scoped rule requires a HOST sponsor" }
            }
            if (scope == "PARTNER") {
                require(sponsor.sponsorType == "PARTNER") { "PARTNER scoped rule requires a PARTNER sponsor" }
            }
        }
        if (scope == "LOCATION") {
            val location = request.locationId?.let { locationRepository.findById(it).orElse(null) }
            require(location != null && location.tenantId == request.tenantId && location.sponsorId == request.sponsorId) {
                "Location does not belong to sponsor"
            }
        }
        val rule = BranchRuleEntity(
            id = 0,
            tenantId = request.tenantId,
            branchId = request.branchId,
            programId = request.programId,
            sponsorId = request.sponsorId,
            locationId = request.locationId,
            scope = scope,
            name = request.name,
            eventType = request.eventType.uppercase(),
            minAmount = request.minAmount,
            rewardType = request.rewardType.uppercase(),
            rewardValue = request.rewardValue,
            isActive = request.isActive,
            priority = request.priority
        )
        return ResponseEntity.ok(BranchRuleResponse.from(branchRuleRepository.save(rule)))
    }

    @GetMapping
    fun listRules(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @RequestParam(defaultValue = "PURCHASE") eventType: String
    ): ResponseEntity<List<BranchRuleResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        return ResponseEntity.ok(branchRuleRepository
            .findByTenantIdAndEventTypeAndIsActiveTrue(tenantId, eventType.uppercase())
            .map(BranchRuleResponse::from))
    }
}
