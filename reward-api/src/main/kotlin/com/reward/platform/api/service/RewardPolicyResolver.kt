package com.reward.platform.api.service

import com.reward.platform.api.entity.BranchRuleEntity
import com.reward.platform.api.repository.BranchRuleRepository
import com.reward.platform.api.repository.SponsorRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class RewardPoints(
    val redemption: Long,
    val recognition: Long,
    val rule: BranchRuleEntity?
)

@Service
class RewardPolicyResolver(
    private val branchRuleRepository: BranchRuleRepository,
    private val sponsorRepository: SponsorRepository
) {

    fun resolveEarnPoints(
        tenantId: Long,
        programId: Long,
        sponsorId: Long,
        locationId: Long?,
        eventType: String,
        amount: Long
    ): RewardPoints {
        val now = Instant.now()
        val sponsor = sponsorRepository.findById(sponsorId).orElse(null)
        val ancestorIds = sponsor?.let(::collectSponsorAncestorIds).orEmpty()
        val matchingRules = branchRuleRepository
            .findByTenantIdAndEventTypeAndIsActiveTrue(tenantId, eventType)
            .asSequence()
            .filter { it.programId == null || it.programId == programId }
            .filter { it.minAmount == null || BigDecimal.valueOf(amount) >= it.minAmount }
            .filter { it.validFrom == null || !it.validFrom.isAfter(now) }
            .filter { it.validUntil == null || !it.validUntil.isBefore(now) }
            .filter {
                when (it.scope) {
                    "LOCATION" -> locationId != null && it.locationId == locationId
                    "SPONSOR" -> it.sponsorId == sponsorId
                    "PARTNER" -> sponsor?.sponsorType == "PARTNER" && it.sponsorId == sponsorId
                    "PARENT" -> it.sponsorId in ancestorIds
                    "PROGRAM" -> true
                    else -> false
                }
            }
            .sortedWith(
                compareByDescending<BranchRuleEntity> { scopePrecedence(it.scope) }
                    .thenByDescending { it.priority }
            )
            .toList()

        val rule = matchingRules.firstOrNull()
        if (rule == null) {
            val defaultPoints = if (eventType == "PURCHASE" && amount > 0) amount / 10 else defaultPointsFor(eventType)
            return RewardPoints(defaultPoints, defaultPoints, null)
        }

        val redemptionPoints = rule.redemptionEarnRate
            ?.let { calculateRatePoints(amount, it) }
            ?: calculateLegacyPoints(amount, rule)
        val recognitionPoints = rule.recognitionEarnRate
            ?.let { calculateRatePoints(amount, it) }
            ?: redemptionPoints

        return RewardPoints(redemptionPoints, recognitionPoints, rule)
    }

    private fun scopePrecedence(scope: String): Int = when (scope) {
        "LOCATION" -> 3
        "SPONSOR", "PARTNER" -> 2
        "PARENT" -> 2
        "PROGRAM" -> 1
        else -> 0
    }

    private fun collectSponsorAncestorIds(sponsor: com.reward.platform.api.entity.SponsorEntity): Set<Long> {
        val ancestors = mutableSetOf<Long>()
        var currentParentId = sponsor.parentSponsorId
        while (currentParentId != null && ancestors.add(currentParentId)) {
            currentParentId = sponsorRepository.findById(currentParentId).orElse(null)?.parentSponsorId
        }
        return ancestors
    }

    private fun calculateRatePoints(amount: Long, rate: BigDecimal): Long =
        BigDecimal.valueOf(amount).multiply(rate).setScale(0, RoundingMode.DOWN).longValueExact()

    private fun calculateLegacyPoints(amount: Long, rule: BranchRuleEntity): Long = when (rule.rewardType) {
        "PERCENTAGE" -> BigDecimal.valueOf(amount)
            .multiply(rule.rewardValue)
            .divide(BigDecimal(100), 0, RoundingMode.DOWN)
            .longValueExact()
        "MULTIPLIER" -> BigDecimal.valueOf(amount)
            .multiply(rule.rewardValue)
            .setScale(0, RoundingMode.DOWN)
            .longValueExact()
        else -> rule.rewardValue.longValueExact()
    }

    private fun defaultPointsFor(eventType: String): Long = when (eventType) {
        "SIGNUP" -> 100
        "REFERRAL" -> 250
        else -> 0
    }
}
