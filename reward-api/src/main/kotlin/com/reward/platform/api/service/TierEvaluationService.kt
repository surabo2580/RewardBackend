package com.reward.platform.api.service

import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.TierRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class TierEvaluationResult(
    val currentTier: String,
    val upgraded: Boolean
)

@Service
class TierEvaluationService(
    private val tierRepository: TierRepository,
    private val memberRepository: MemberRepository,
    private val bitService: BitService
) {

    fun currentMultiplier(member: MemberEntity, programId: Long): BigDecimal =
        tierRepository
            .findByTenantIdAndProgramIdOrderByRank(member.tenantId, programId)
            .firstOrNull { it.name == member.tier }
            ?.multiplier
            ?: BigDecimal.ONE

    fun currentRank(member: MemberEntity, programId: Long): Int =
        tierRepository
            .findByTenantIdAndProgramIdOrderByRank(member.tenantId, programId)
            .firstOrNull { it.name == member.tier }
            ?.rank
            ?: 0

    fun evaluate(member: MemberEntity, programId: Long, recognitionPoints: Long): TierEvaluationResult {
        val eligibleTier = tierRepository
            .findByTenantIdAndProgramIdOrderByRank(member.tenantId, programId)
            .filter { recognitionPoints >= it.thresholdPoints }
            .maxByOrNull { it.rank }
            ?: return TierEvaluationResult(member.tier, false)

        val upgraded = member.tier != eligibleTier.name
        if (upgraded) {
            memberRepository.save(member.copy(tier = eligibleTier.name))
            bitService.record(
                BitCommand(
                    tenantId = member.tenantId,
                    memberId = member.id,
                    bitType = com.reward.platform.api.entity.BitType.TIER_CHANGE,
                    programId = programId,
                    channel = "SYSTEM",
                    description = "Tier qualified ${member.tier} → ${eligibleTier.name}",
                    payload = mapOf("previousTier" to member.tier, "currentTier" to eligibleTier.name, "recognitionPoints" to recognitionPoints, "source" to "QUALIFICATION")
                )
            )
        }

        return TierEvaluationResult(eligibleTier.name, upgraded)
    }
}
