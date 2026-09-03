package com.reward.platform.api.controller

import com.reward.platform.api.dto.OfferCreateRequest
import com.reward.platform.api.dto.OfferResponse
import com.reward.platform.api.dto.OfferStatusUpdateRequest
import com.reward.platform.api.entity.OfferEntity
import com.reward.platform.api.entity.OfferSponsorEntity
import com.reward.platform.api.entity.OfferTargetMemberEntity
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.OfferSponsorRepository
import com.reward.platform.api.repository.OfferTargetMemberRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SponsorRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val offerSponsorRepository: OfferSponsorRepository,
    private val offerTargetMemberRepository: OfferTargetMemberRepository,
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
        val category = request.category.uppercase()
        require(category in setOf("AWARD", "REWARD", "PRIVILEGE", "DEAL")) { "Invalid offer category" }
        val status = request.status.uppercase()
        require(status in setOf("DRAFT", "SCHEDULED", "LAUNCHED", "PAUSED", "EXPIRED", "ARCHIVED")) { "Invalid offer status" }
        val offerType = request.offerType.uppercase()
        require(offerType in setOf("MULTIPLIER", "BONUS_POINTS", "HYBRID")) { "Invalid offer type" }
        require(!request.endDate.isBefore(request.startDate)) { "Offer end date must not be before start date" }
        require(request.multiplier >= java.math.BigDecimal.ONE) { "Offer multiplier must be at least 1" }
        require(request.maxUsesPerMember == null || request.maxUsesPerMember > 0) { "Maximum uses must be greater than zero" }
        require(request.maxTotalClaims == null || request.maxTotalClaims > 0) { "Maximum total claims must be greater than zero" }
        require(!request.isMto || request.targetMemberIds.isNotEmpty()) { "Member-targeted offers require at least one target member" }
        if (scope != "PROGRAM") {
            val sponsor = request.sponsorId?.let { sponsorRepository.findById(it).orElse(null) }
            require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) { "Sponsor does not belong to tenant and program" }
        }
        if (scope == "LOCATION") {
            val location = request.locationId?.let { locationRepository.findById(it).orElse(null) }
            require(location != null && location.tenantId == request.tenantId && location.sponsorId == request.sponsorId) { "Location does not belong to sponsor" }
        }
        val sponsorIds = (request.sponsorIds + listOfNotNull(request.sponsorId)).distinct()
        sponsorIds.forEach { sponsorId ->
            val sponsor = sponsorRepository.findById(sponsorId).orElse(null)
            require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) { "Sponsor does not belong to tenant and program" }
        }
        val offer = OfferEntity(
            tenantId = request.tenantId, programId = request.programId, offerCode = request.offerCode.trim().uppercase(), name = request.name.trim(), description = request.description?.trim(),
            category = category, status = status,
            scope = scope, sponsorId = request.sponsorId, locationId = request.locationId, offerType = offerType,
            multiplier = request.multiplier, bonusPoints = request.bonusPoints, minSpend = request.minSpend,
            minTierRank = request.minTierRank, eligibleDays = request.eligibleDays?.trim(), maxUsesPerMember = request.maxUsesPerMember, maxTotalClaims = request.maxTotalClaims,
            isMto = request.isMto, isFeatured = request.isFeatured, pointsRequired = request.pointsRequired, benefitCode = request.benefitCode?.trim(), targetTierId = request.targetTierId,
            discountType = request.discountType?.uppercase(), discountValue = request.discountValue, promoCode = request.promoCode?.trim(),
            startDate = request.startDate, endDate = request.endDate, isActive = request.isActive
        )
        val savedOffer = offerRepository.save(offer)
        offerSponsorRepository.saveAll(sponsorIds.map { OfferSponsorEntity(offerId = savedOffer.id, sponsorId = it) })
        offerTargetMemberRepository.saveAll(request.targetMemberIds.distinct().map { OfferTargetMemberEntity(offerId = savedOffer.id, memberId = it) })
        return ResponseEntity.ok(OfferResponse.from(savedOffer, sponsorIds))
    }

    @GetMapping
    fun list(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @RequestParam programId: Long
    ): ResponseEntity<List<OfferResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        val offers = offerRepository.findByTenantIdAndProgramIdOrderByCreatedAtDesc(tenantId, programId)
        val sponsorIdsByOffer = offerSponsorRepository.findByOfferIdIn(offers.map { it.id }).groupBy({ it.offerId }, { it.sponsorId })
        return ResponseEntity.ok(offers.map { OfferResponse.from(it, sponsorIdsByOffer[it.id].orEmpty()) })
    }

    @PatchMapping("/{offerId}/status")
    fun updateStatus(@RequestAttribute("tenantId") tenantId: Long, @PathVariable offerId: Long, @Valid @RequestBody request: OfferStatusUpdateRequest): ResponseEntity<OfferResponse> {
        val status = request.status.uppercase()
        require(status in setOf("DRAFT", "SCHEDULED", "LAUNCHED", "PAUSED", "EXPIRED", "ARCHIVED")) { "Invalid offer status" }
        val offer = offerRepository.findByTenantIdAndId(tenantId, offerId) ?: throw IllegalArgumentException("Offer not found")
        val updated = offerRepository.save(offer.copy(status = status, isActive = status == "LAUNCHED"))
        return ResponseEntity.ok(OfferResponse.from(updated, offerSponsorRepository.findByOfferId(updated.id).map { it.sponsorId }))
    }

    @PatchMapping("/{offerId}/featured")
    fun toggleFeatured(@RequestAttribute("tenantId") tenantId: Long, @PathVariable offerId: Long): ResponseEntity<OfferResponse> {
        val offer = offerRepository.findByTenantIdAndId(tenantId, offerId) ?: throw IllegalArgumentException("Offer not found")
        val updated = offerRepository.save(offer.copy(isFeatured = !offer.isFeatured))
        return ResponseEntity.ok(OfferResponse.from(updated, offerSponsorRepository.findByOfferId(updated.id).map { it.sponsorId }))
    }
}