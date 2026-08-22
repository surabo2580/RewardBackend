package com.reward.platform.api.controller

import com.reward.platform.api.dto.BranchRuleCreateRequest
import com.reward.platform.api.dto.BranchRuleResponse
import com.reward.platform.api.entity.BranchRuleEntity
import com.reward.platform.api.repository.BranchRuleRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/rules")
class BranchRuleController(
    private val branchRuleRepository: BranchRuleRepository
) {

    @PostMapping
    fun createRule(@Valid @RequestBody request: BranchRuleCreateRequest): ResponseEntity<BranchRuleResponse> {
        val rule = BranchRuleEntity(
            id = UUID.randomUUID().toString(),
            tenantId = request.tenantId,
            branchId = request.branchId,
            programId = request.programId,
            name = request.name,
            eventType = request.eventType.uppercase(),
            minAmount = request.minAmount,
            rewardType = request.rewardType.uppercase(),
            rewardValue = request.rewardValue,
            isActive = request.isActive
        )
        return ResponseEntity.ok(BranchRuleResponse.from(branchRuleRepository.save(rule)))
    }

    @GetMapping
    fun listRules(
        @RequestParam tenantId: String,
        @RequestParam(defaultValue = "PURCHASE") eventType: String
    ): ResponseEntity<List<BranchRuleResponse>> {
        return ResponseEntity.ok(
            branchRuleRepository.findByTenantIdAndEventTypeAndIsActiveTrue(tenantId, eventType.uppercase())
                .map(BranchRuleResponse::from)
        )
    }
}
