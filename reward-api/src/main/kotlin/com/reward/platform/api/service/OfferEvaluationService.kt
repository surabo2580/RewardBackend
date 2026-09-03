package com.reward.platform.api.service

import com.reward.platform.api.entity.OfferEntity
import com.reward.platform.api.repository.OfferApplicationRepository
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.OfferSponsorRepository
import com.reward.platform.api.repository.OfferTargetMemberRepository
import com.reward.platform.api.repository.SponsorRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

data class OfferEvaluationResult(
    val multiplier: BigDecimal = BigDecimal.ONE,
    val bonusPoints: Long = 0,
    val offerIds: List<Long> = emptyList()
)

@Service
class OfferEvaluationService(
    private val offerRepository: OfferRepository,
    private val offerApplicationRepository: OfferApplicationRepository,
    private val offerSponsorRepository: OfferSponsorRepository,
    private val offerTargetMemberRepository: OfferTargetMemberRepository,
    private val sponsorRepository: SponsorRepository
) {
    fun evaluate(
        tenantId: Long,
        programId: Long,
        memberId: Long,
        sponsorId: Long,
        locationId: Long,
        tierRank: Int,
        amount: Long,
        occurredAt: Instant = Instant.now()
    ): OfferEvaluationResult {
        val sponsor = sponsorRepository.findById(sponsorId).orElse(null)
        val ancestorIds = sponsor?.let(::collectAncestorIds).orEmpty()
        val day = occurredAt.atZone(ZoneOffset.UTC).dayOfWeek.name
        val matchingOffers = offerRepository
            .findByTenantIdAndProgramIdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                tenantId, programId, occurredAt, occurredAt
            )
            .filter { it.category == "AWARD" && it.status == "LAUNCHED" }
            .filter { isInScope(it, sponsorId, locationId, sponsor?.sponsorType, ancestorIds) }
            .filter { BigDecimal.valueOf(amount) >= it.minSpend }
            .filter { tierRank >= it.minTierRank }
            .filter { offer -> offer.eligibleDays.isNullOrBlank() || day in offer.eligibleDays.split(',').map { it.trim().uppercase() } }
            .filter { offer -> !offer.isMto || offerTargetMemberRepository.existsByOfferIdAndMemberId(offer.id, memberId) }
            .filter { offer -> offer.maxUsesPerMember == null || offerApplicationRepository.countByTenantIdAndMemberIdAndOfferId(tenantId, memberId, offer.id) < offer.maxUsesPerMember }
            .filter { offer -> offer.maxTotalClaims == null || offer.totalClaimsCount < offer.maxTotalClaims }

        return OfferEvaluationResult(
            multiplier = matchingOffers.maxOfOrNull { it.multiplier } ?: BigDecimal.ONE,
            bonusPoints = matchingOffers.sumOf { it.bonusPoints },
            offerIds = matchingOffers.map { it.id }
        )
    }

    private fun isInScope(offer: OfferEntity, sponsorId: Long, locationId: Long, sponsorType: String?, ancestorIds: Set<Long>) = when (offer.scope) {
        "PROGRAM" -> true
        "SPONSOR" -> sponsorMatches(offer, sponsorId)
        "LOCATION" -> offer.locationId == locationId
        "PARENT" -> offer.sponsorId in ancestorIds
        "PARTNER" -> sponsorType == "PARTNER" && sponsorMatches(offer, sponsorId)
        else -> false
    }

    private fun sponsorMatches(offer: OfferEntity, sponsorId: Long): Boolean =
        offer.sponsorId == sponsorId || offerSponsorRepository.findByOfferId(offer.id).any { it.sponsorId == sponsorId }

    private fun collectAncestorIds(sponsor: com.reward.platform.api.entity.SponsorEntity): Set<Long> {
        val ancestors = mutableSetOf<Long>()
        var parentId = sponsor.parentSponsorId
        while (parentId != null && ancestors.add(parentId)) {
            parentId = sponsorRepository.findById(parentId).orElse(null)?.parentSponsorId
        }
        return ancestors
    }
}