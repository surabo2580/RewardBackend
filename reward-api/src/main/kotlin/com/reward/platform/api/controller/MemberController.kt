package com.reward.platform.api.controller

import com.reward.platform.api.dto.MemberCreateRequest
import com.reward.platform.api.dto.MemberImportJobResponse
import com.reward.platform.api.dto.MemberResponse
import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.repository.MemberImportJobRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.service.MemberImportService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.multipart.MultipartFile

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberRepository: MemberRepository,
    private val memberImportService: MemberImportService,
    private val memberImportJobRepository: MemberImportJobRepository
) {

    @PostMapping
    fun createMember(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: MemberCreateRequest
    ): ResponseEntity<MemberResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        val entity = MemberEntity(
            id = 0,
            tenantId = request.tenantId,
            externalUserId = request.externalUserId,
            email = request.email,
            tier = request.tier
        )
        return ResponseEntity.ok(MemberResponse.from(memberRepository.save(entity)))
    }

    @GetMapping
    fun listMembers(@RequestAttribute("tenantId") tenantId: Long): ResponseEntity<List<MemberResponse>> {
        return ResponseEntity.ok(memberRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).map(MemberResponse::from))
    }

    @PostMapping("/imports", consumes = ["multipart/form-data"])
    fun startImport(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<MemberImportJobResponse> =
        ResponseEntity.accepted().body(MemberImportJobResponse.from(memberImportService.queue(tenantId, file)))

    @GetMapping("/imports")
    fun listImports(@RequestAttribute("tenantId") tenantId: Long): ResponseEntity<List<MemberImportJobResponse>> =
        ResponseEntity.ok(memberImportJobRepository.findTop20ByTenantIdOrderByCreatedAtDesc(tenantId).map(MemberImportJobResponse::from))

    @GetMapping("/imports/{jobId}")
    fun getImport(@RequestAttribute("tenantId") tenantId: Long, @PathVariable jobId: Long): ResponseEntity<MemberImportJobResponse> {
        val job = memberImportJobRepository.findByTenantIdAndId(tenantId, jobId) ?: throw IllegalArgumentException("Import job not found")
        return ResponseEntity.ok(MemberImportJobResponse.from(job))
    }
}