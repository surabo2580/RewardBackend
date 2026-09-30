package com.reward.platform.api.controller

import com.reward.platform.api.dto.BitCreateRequest
import com.reward.platform.api.dto.BitDetailResponse
import com.reward.platform.api.dto.BitPageResponse
import com.reward.platform.api.dto.BitResponse
import com.reward.platform.api.dto.MemberBitSummaryResponse
import com.reward.platform.api.entity.BitCategory
import com.reward.platform.api.entity.BitType
import com.reward.platform.api.service.BitFilter
import com.reward.platform.api.service.BitQueryService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api")
class BitController(
    private val bitQueryService: BitQueryService
) {

    @GetMapping("/bits/types")
    fun types(): ResponseEntity<List<Map<String, String>>> =
        ResponseEntity.ok(BitType.entries.map { mapOf("type" to it.name, "label" to it.label, "category" to it.category.name) })

    @GetMapping("/bits/categories")
    fun categories(): ResponseEntity<List<String>> = ResponseEntity.ok(BitCategory.entries.map { it.name })

    /** Records a non-monetary or travel touchpoint (app login, survey, referral, stay, profile update). */
    @PostMapping("/bits")
    fun record(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: BitCreateRequest
    ): ResponseEntity<BitResponse> = ResponseEntity.status(HttpStatus.CREATED).body(bitQueryService.recordDirect(tenantId, request))

    @GetMapping("/bits")
    fun list(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestParam(required = false) memberId: Long?,
        @RequestParam(required = false) bitType: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) sponsorId: Long?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<BitPageResponse> = ResponseEntity.ok(
        bitQueryService.list(tenantId, BitFilter(memberId, bitType, category, sponsorId, status, from, to), page, size)
    )

    @GetMapping("/bits/{bitId}")
    fun detail(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable bitId: Long
    ): ResponseEntity<BitDetailResponse> = ResponseEntity.ok(bitQueryService.detail(tenantId, bitId))

    @GetMapping("/members/{memberId:\\d+}/bits")
    fun memberBits(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<BitResponse>> = ResponseEntity.ok(bitQueryService.memberBits(tenantId, memberId, limit))

    @GetMapping("/members/{memberId:\\d+}/bits/summary")
    fun memberSummary(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<MemberBitSummaryResponse> = ResponseEntity.ok(bitQueryService.memberSummary(tenantId, memberId))
}
