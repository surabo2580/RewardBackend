package com.reward.platform.api.controller

import com.reward.platform.api.dto.OfferCreateRequest
import com.reward.platform.api.dto.OfferKpiTarget
import com.reward.platform.api.dto.OfferResponse
import com.reward.platform.api.dto.OfferSimulationRequest
import com.reward.platform.api.dto.OfferSimulationResponse
import com.reward.platform.api.dto.OfferStatusUpdateRequest
import com.reward.platform.api.entity.OfferEntity
import com.reward.platform.api.entity.OfferKpiEntity
import com.reward.platform.api.entity.OfferLocationEntity
import com.reward.platform.api.entity.OfferSponsorEntity
import com.reward.platform.api.entity.OfferTargetMemberEntity
import com.reward.platform.api.repository.OfferKpiRepository
import com.reward.platform.api.repository.OfferLocationRepository
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.OfferSponsorRepository
import com.reward.platform.api.repository.OfferTargetMemberRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.service.OfferSimulationService
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
import java.math.BigDecimal

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/offers")
class OfferController(
    private val offerRepository: OfferRepository,
    private val offerSponsorRepository: OfferSponsorRepository,
    private val offerLocationRepository: OfferLocationRepository,
    private val offerKpiRepository: OfferKpiRepository,
    private val offerTargetMemberRepository: OfferTargetMemberRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository,
    private val offerSimulationService: OfferSimulationService
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
        val billingType = request.billingType.uppercase()
        require(billingType in setOf("BILLING_SPONSOR", "BIT_SPONSOR")) { "Billing type must be BILLING_SPONSOR or BIT_SPONSOR" }
        val offerVisibility = request.offerVisibility.uppercase()
        require(offerVisibility in setOf("ON_OFFER_LAUNCH", "ON_ACTIVATION", "HIDDEN")) { "Invalid offer visibility" }
        val targetAccount = request.targetAccount.uppercase()
        require(targetAccount in setOf("REDEMPTION", "RECOGNITION", "BOTH")) { "Invalid target account" }
        require(!request.endDate.isBefore(request.startDate)) { "Offer end date must not be before start date" }
        require(request.multiplier >= BigDecimal.ONE) { "Offer multiplier must be at least 1" }
        require(request.maxUsesPerMember == null || request.maxUsesPerMember > 0) { "Maximum uses must be greater than zero" }
        require(request.maxTotalClaims == null || request.maxTotalClaims > 0) { "Maximum total claims must be greater than zero" }
        require(request.maxRewardLimitPoints == null || request.maxRewardLimitPoints > 0) { "Maximum reward limit must be greater than zero" }
        require(!request.isMto || request.targetMemberIds.isNotEmpty()) { "Member-targeted offers require at least one target member" }
        if (category == "REWARD") require(request.pointsRequired > 0) { "Reward offers require the points needed to claim" }
        if (category == "DEAL") {
            require(request.discountType?.uppercase() in setOf("PERCENTAGE", "FIXED_AMOUNT")) { "Deal offers require a discount type" }
            require((request.discountValue ?: BigDecimal.ZERO) > BigDecimal.ZERO) { "Deal offers require a discount value" }
        }
        if (category == "PRIVILEGE") {
            require(!request.benefitCode.isNullOrBlank() || request.targetTierId != null) { "Privilege offers require a benefit code or a target tier" }
        }
        if (scope != "PROGRAM") {
            val sponsor = request.sponsorId?.let { sponsorRepository.findById(it).orElse(null) }
            require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) { "Sponsor does not belong to tenant and program" }
        }
        if (scope == "LOCATION") {
            val location = request.locationId?.let { locationRepository.findById(it).orElse(null) }
            require(location != null && location.tenantId == request.tenantId && location.sponsorId == request.sponsorId) { "Location does not belong to sponsor" }
        }
        val bitSponsorIds = (request.bitSponsorIds + request.sponsorIds + listOfNotNull(request.sponsorId)).distinct()
        bitSponsorIds.forEach { sponsorId ->
            val sponsor = sponsorRepository.findById(sponsorId).orElse(null)
            require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) { "Sponsor does not belong to tenant and program" }
        }
        val billingSponsorId = request.billingSponsorId?.takeIf { billingType == "BILLING_SPONSOR" }
        if (billingSponsorId != null) {
            val billingSponsor = sponsorRepository.findById(billingSponsorId).orElse(null)
            require(billingSponsor != null && billingSponsor.tenantId == request.tenantId && billingSponsor.programId == request.programId) { "Billing sponsor does not belong to tenant and program" }
        }
        val locationIds = (request.locationIds + listOfNotNull(request.locationId)).distinct()
        require(request.allLocations || locationIds.isNotEmpty()) { "Select at least one location or enable all locations" }
        locationIds.forEach { locationId ->
            val location = locationRepository.findById(locationId).orElse(null)
            require(location != null && location.tenantId == request.tenantId) { "Location does not belong to tenant" }
        }
        val kpis = request.kpis.filter { it.kpiCode.isNotBlank() }.distinctBy { it.kpiCode.uppercase() }
        val offer = OfferEntity(
            tenantId = request.tenantId, programId = request.programId, offerCode = request.offerCode.trim().uppercase(), name = request.name.trim(),
            subtitle = request.subtitle?.trim(), description = request.description?.trim(),
            category = category, status = status,
            scope = scope, sponsorId = request.sponsorId, locationId = request.locationId, allLocations = request.allLocations,
            billingType = billingType, billingSponsorId = billingSponsorId,
            memberVisibility = request.memberVisibility, offerVisibility = offerVisibility,
            maxRewardLimitPoints = request.maxRewardLimitPoints, requiresAcceptance = request.requiresAcceptance,
            targetAccount = targetAccount, fulfillmentType = request.fulfillmentType?.trim()?.uppercase(), offerType = offerType,
            multiplier = request.multiplier, bonusPoints = request.bonusPoints, minSpend = request.minSpend,
            minTierRank = request.minTierRank, eligibleDays = request.eligibleDays?.trim(), maxUsesPerMember = request.maxUsesPerMember, maxTotalClaims = request.maxTotalClaims,
            isMto = request.isMto, isFeatured = request.isFeatured, pointsRequired = request.pointsRequired, benefitCode = request.benefitCode?.trim(), targetTierId = request.targetTierId,
            discountType = request.discountType?.uppercase(), discountValue = request.discountValue, promoCode = request.promoCode?.trim(),
            startDate = request.startDate, endDate = request.endDate, isActive = request.isActive && status == "LAUNCHED"
        )
        val savedOffer = offerRepository.save(offer)
        offerSponsorRepository.saveAll(bitSponsorIds.map { OfferSponsorEntity(offerId = savedOffer.id, sponsorId = it) })
        offerLocationRepository.saveAll(locationIds.map { OfferLocationEntity(offerId = savedOffer.id, locationId = it) })
        offerKpiRepository.saveAll(kpis.map { OfferKpiEntity(offerId = savedOffer.id, kpiCode = it.kpiCode.trim().uppercase(), targetValue = it.targetValue) })
        val targetMemberIds = request.targetMemberIds.distinct()
        offerTargetMemberRepository.saveAll(targetMemberIds.map { OfferTargetMemberEntity(offerId = savedOffer.id, memberId = it) })
        return ResponseEntity.ok(OfferResponse.from(savedOffer, bitSponsorIds, locationIds, targetMemberIds, kpis))
    }

    @PostMapping("/simulate")
    fun simulate(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestBody request: OfferSimulationRequest
    ): ResponseEntity<OfferSimulationResponse> = ResponseEntity.ok(offerSimulationService.simulate(request))

    @GetMapping
    fun list(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @RequestParam programId: Long
    ): ResponseEntity<List<OfferResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        val offers = offerRepository.findByTenantIdAndProgramIdOrderByCreatedAtDesc(tenantId, programId)
        val offerIds = offers.map { it.id }
        val sponsorIdsByOffer = offerSponsorRepository.findByOfferIdIn(offerIds).groupBy({ it.offerId }, { it.sponsorId })
        val locationIdsByOffer = offerLocationRepository.findByOfferIdIn(offerIds).groupBy({ it.offerId }, { it.locationId })
        val memberIdsByOffer = offerTargetMemberRepository.findByOfferIdIn(offerIds).groupBy({ it.offerId }, { it.memberId })
        val kpisByOffer = offerKpiRepository.findByOfferIdIn(offerIds).groupBy({ it.offerId }, { OfferKpiTarget(it.kpiCode, it.targetValue) })
        return ResponseEntity.ok(offers.map {
            OfferResponse.from(it, sponsorIdsByOffer[it.id].orEmpty(), locationIdsByOffer[it.id].orEmpty(), memberIdsByOffer[it.id].orEmpty(), kpisByOffer[it.id].orEmpty())
        })
    }

    @PatchMapping("/{offerId}/status")
    fun updateStatus(@RequestAttribute("tenantId") tenantId: Long, @PathVariable offerId: Long, @Valid @RequestBody request: OfferStatusUpdateRequest): ResponseEntity<OfferResponse> {
        val status = request.status.uppercase()
        require(status in setOf("DRAFT", "SCHEDULED", "LAUNCHED", "PAUSED", "EXPIRED", "ARCHIVED")) { "Invalid offer status" }
        val offer = offerRepository.findByTenantIdAndId(tenantId, offerId) ?: throw IllegalArgumentException("Offer not found")
        val updated = offerRepository.save(offer.copy(status = status, isActive = status == "LAUNCHED"))
        return ResponseEntity.ok(describe(updated))
    }

    @PatchMapping("/{offerId}/featured")
    fun toggleFeatured(@RequestAttribute("tenantId") tenantId: Long, @PathVariable offerId: Long): ResponseEntity<OfferResponse> {
        val offer = offerRepository.findByTenantIdAndId(tenantId, offerId) ?: throw IllegalArgumentException("Offer not found")
        val updated = offerRepository.save(offer.copy(isFeatured = !offer.isFeatured))
        return ResponseEntity.ok(describe(updated))
    }

    private fun describe(offer: OfferEntity) = OfferResponse.from(
        offer,
        offerSponsorRepository.findByOfferId(offer.id).map { it.sponsorId },
        offerLocationRepository.findByOfferId(offer.id).map { it.locationId },
        offerTargetMemberRepository.findByOfferId(offer.id).map { it.memberId },
        offerKpiRepository.findByOfferId(offer.id).map { OfferKpiTarget(it.kpiCode, it.targetValue) }
    )
}
