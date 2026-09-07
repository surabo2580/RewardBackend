package com.reward.platform.api.service

import com.reward.platform.api.dto.OfferSimulationCheck
import com.reward.platform.api.dto.OfferSimulationRequest
import com.reward.platform.api.dto.OfferSimulationResponse
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneOffset

/** Dry-runs a draft offer against a sample transaction without writing any records. */
@Service
class OfferSimulationService {
    fun simulate(request: OfferSimulationRequest): OfferSimulationResponse {
        val amount = BigDecimal.valueOf(request.sampleAmount)
        val day = request.sampleOccurredAt.atZone(ZoneOffset.UTC).dayOfWeek.name
        val eligibleDays = request.eligibleDays?.split(',')?.map { it.trim().uppercase() }?.filter { it.isNotBlank() }.orEmpty()
        val bitSponsorIds = request.bitSponsorIds + listOfNotNull(request.sponsorId)

        val checks = buildList {
            add(
                OfferSimulationCheck(
                    "Validity window",
                    !request.sampleOccurredAt.isBefore(request.startDate) && !request.sampleOccurredAt.isAfter(request.endDate),
                    "Transaction at ${request.sampleOccurredAt} vs ${request.startDate} - ${request.endDate}"
                )
            )
            add(
                OfferSimulationCheck(
                    "Sponsor scope",
                    when (request.scope.uppercase()) {
                        "PROGRAM" -> true
                        else -> request.sampleSponsorId != null && request.sampleSponsorId in bitSponsorIds
                    },
                    if (request.scope.uppercase() == "PROGRAM") "Program-wide offer applies at every BIT sponsor"
                    else "BIT sponsors ${bitSponsorIds.joinToString()} vs transacting sponsor ${request.sampleSponsorId ?: "-"}"
                )
            )
            add(
                OfferSimulationCheck(
                    "Location scope",
                    request.allLocations || (request.sampleLocationId != null && request.sampleLocationId in request.locationIds),
                    if (request.allLocations) "All locations selected" else "Allowed locations ${request.locationIds.joinToString()}"
                )
            )
            add(OfferSimulationCheck("Minimum spend", amount >= request.minSpend, "Bill $amount vs minimum ${request.minSpend}"))
            add(OfferSimulationCheck("Tier eligibility", request.sampleTierRank >= request.minTierRank, "Member rank ${request.sampleTierRank} vs required ${request.minTierRank}"))
            add(OfferSimulationCheck("Eligible days", eligibleDays.isEmpty() || day in eligibleDays, if (eligibleDays.isEmpty()) "Every day" else "$day vs ${eligibleDays.joinToString()}"))
            add(
                OfferSimulationCheck(
                    "Member targeting",
                    !request.isMto || (request.sampleMemberId != null && request.sampleMemberId in request.targetMemberIds),
                    if (request.isMto) "Targeted to ${request.targetMemberIds.size} member(s)" else "Open to all enrolled members"
                )
            )
        }

        val qualifies = checks.all { it.passed }
        val category = request.category.uppercase()
        val basePoints = amount.multiply(request.basePointsPerUnit).setScale(0, RoundingMode.DOWN).toLong()
        val awardedRaw = if (qualifies && category == "AWARD") {
            basePoints.toBigDecimal().multiply(request.multiplier).setScale(0, RoundingMode.DOWN).toLong() + request.bonusPoints
        } else if (category == "AWARD") basePoints else 0
        val awarded = request.maxRewardLimitPoints?.let { minOf(awardedRaw, it) } ?: awardedRaw
        val pointsBurned = if (qualifies && category == "REWARD") request.pointsRequired else 0
        val discount = if (qualifies && category == "DEAL") {
            when (request.discountType?.uppercase()) {
                "PERCENTAGE" -> amount.multiply(request.discountValue ?: BigDecimal.ZERO).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                "FIXED_AMOUNT" -> (request.discountValue ?: BigDecimal.ZERO).min(amount)
                else -> BigDecimal.ZERO
            }
        } else BigDecimal.ZERO

        val summary = when {
            !qualifies -> "Offer does not trigger for this sample transaction."
            category == "AWARD" -> "Member earns $awarded points (base $basePoints x ${request.multiplier} + ${request.bonusPoints} bonus)."
            category == "REWARD" -> "Member burns $pointsBurned points and receives the configured fulfilment."
            category == "PRIVILEGE" -> "Member unlocks the configured privilege benefit."
            else -> "Member receives a discount of $discount on a $amount bill."
        }

        return OfferSimulationResponse(
            qualifies = qualifies,
            checks = checks,
            basePoints = basePoints,
            bonusPoints = if (category == "AWARD" && qualifies) request.bonusPoints else 0,
            totalPoints = awarded,
            pointsBurned = pointsBurned,
            discountAmount = discount,
            netPayableAmount = amount.subtract(discount).max(BigDecimal.ZERO),
            summary = summary
        )
    }
}
