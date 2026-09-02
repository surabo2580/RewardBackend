package com.reward.platform.api.controller

import com.reward.platform.api.dto.PartnerMembershipCreateRequest
import com.reward.platform.api.dto.PartnerMembershipResponse
import com.reward.platform.api.entity.PartnerMembershipEntity
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.PartnerMembershipRepository
import com.reward.platform.api.repository.SponsorRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/partner-memberships")
class PartnerMembershipController(
    private val partnerMembershipRepository: PartnerMembershipRepository,
    private val sponsorRepository: SponsorRepository,
    private val memberRepository: MemberRepository
) {

    @PostMapping
    fun createPartnerMembership(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: PartnerMembershipCreateRequest
    ): ResponseEntity<PartnerMembershipResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        val sponsor = sponsorRepository.findById(request.sponsorId).orElse(null)
        require(sponsor != null && sponsor.tenantId == request.tenantId) { "Sponsor does not belong to tenant" }
        require(sponsor.sponsorType == "PARTNER") { "Sponsor must be PARTNER type" }

        val member = memberRepository.findByTenantIdAndExternalUserId(request.tenantId, request.memberId)
        require(member != null) { "Member not found for tenant" }

        val membership = PartnerMembershipEntity(
            tenantId = request.tenantId,
            sponsorId = request.sponsorId,
            memberId = member.id,
            externalMembershipId = request.externalMembershipId,
            status = request.status
        )
        return ResponseEntity.ok(PartnerMembershipResponse.from(partnerMembershipRepository.save(membership)))
    }

    @GetMapping
    fun listPartnerMemberships(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @RequestParam sponsorId: Long
    ): ResponseEntity<List<PartnerMembershipResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        val sponsor = sponsorRepository.findById(sponsorId).orElse(null)
        require(sponsor != null && sponsor.tenantId == tenantId) { "Sponsor does not belong to tenant" }
        return ResponseEntity.ok(
            partnerMembershipRepository
                .findByTenantIdAndSponsorIdOrderByCreatedAtDesc(tenantId, sponsorId)
                .map(PartnerMembershipResponse::from)
        )
    }
}