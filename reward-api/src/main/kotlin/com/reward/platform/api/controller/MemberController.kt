package com.reward.platform.api.controller

import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.repository.MemberRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberRepository: MemberRepository
) {

    @PostMapping
    fun createMember(@RequestBody member: MemberEntity): ResponseEntity<MemberEntity> {
        return ResponseEntity.ok(memberRepository.save(member))
    }

    @GetMapping
    fun listMembers(): ResponseEntity<List<MemberEntity>> {
        return ResponseEntity.ok(memberRepository.findAll())
    }
}
