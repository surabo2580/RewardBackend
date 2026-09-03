package com.reward.platform.api.controller

import com.reward.platform.api.dto.OfferCreateRequest
import com.reward.platform.api.dto.OfferResponse
import com.reward.platform.api.entity.OfferEntity
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorLocationRepository
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
@RequestMapping("/api/offers")
class OfferController(
    private val offerRepository: OfferRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository
) {
    @PostMapping
    fun create(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @Valid @RequestBody request: OfferCreateRequest
    ): ResponseEntity<OfferResponse> {
        require(request.tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        require(programRepository.findById(request.programId).orElse(null)?.tenantId == request.tenantId) { "Program does not belong to tenant" }
        val scope = request.scope.uppercase()
        require(scope in setOf("PROGRAM", "SPONSOR", "LOCATION", "PARENT", "PARTNER")) { "Invalid offer scope" }
        val offerType = request.offerType.uppercase()
        require(offerType in setOf("MULTIPLIER", "BONUS_POINTS", "HYBRID")) { "Invalid offer type" }
        require(!request.endDate.isBefore(request.startDate)) { "Offer end date must not be before start date" }
        require(request.multiplier >= java.math.BigDecimal.ONE) { "Offer multiplier must be at least 1" }
        require(request.maxUsesPerMember == null || request.maxUsesPerMember > 0) { "Maximum uses must be greater than zero" }
        if (scope != "PROGRAM") {
            val sponsor = request.sponsorId?.let { sponsorRepository.findById(it).orElse(null) }
            require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) { "Sponsor does not belong to tenant and program" }
        }
        if (scope == "LOCATION") {
            val location = request.locationId?.let { locationRepository.findById(it).orElse(null) }
            require(location != null && location.tenantId == request.tenantId && location.sponsorId == request.sponsorId) { "Location does not belong to sponsor" }
        }
        val offer = OfferEntity(
            tenantId = request.tenantId, programId = request.programId, name = request.name.trim(), description = request.description?.trim(),
            scope = scope, sponsorId = request.sponsorId, locationId = request.locationId, offerType = offerType,
            multiplier = request.multiplier, bonusPoints = request.bonusPoints, minSpend = request.minSpend,
            minTierRank = request.minTierRank, eligibleDays = request.eligibleDays?.trim(), maxUsesPerMember = request.maxUsesPerMember,
            startDate = request.startDate, endDate = request.endDate, isActive = request.isActive
        )
        return ResponseEntity.ok(OfferResponse.from(offerRepository.save(offer)))
    }

    @GetMapping
    fun list(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @RequestParam programId: Long
    ): ResponseEntity<List<OfferResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        return ResponseEntity.ok(offerRepository.findByTenantIdAndProgramIdOrderByCreatedAtDesc(tenantId, programId).map(OfferResponse::from))
    }
}