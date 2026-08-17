package com.reward.platform.api.controller

import com.reward.platform.api.dto.MemberCreateRequest
import com.reward.platform.api.dto.MemberResponse
import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.repository.MemberRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberRepository: MemberRepository
) {

    @PostMapping
    fun createMember(@Valid @RequestBody request: MemberCreateRequest): ResponseEntity<MemberResponse> {
        val entity = MemberEntity(
            id = UUID.randomUUID().toString(),
            tenantId = request.tenantId,
            externalUserId = request.externalUserId,
            email = request.email,
            tier = request.tier
        )
        return ResponseEntity.ok(MemberResponse.from(memberRepository.save(entity)))
    }

    @GetMapping
    fun listMembers(): ResponseEntity<List<MemberResponse>> {
        return ResponseEntity.ok(memberRepository.findAll().map(MemberResponse::from))
    }
}
