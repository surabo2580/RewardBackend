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
    private val memberRepository: MemberRepository
) {

    fun currentMultiplier(member: MemberEntity, programId: Long): BigDecimal =
        tierRepository
            .findByTenantIdAndProgramIdOrderByRank(member.tenantId, programId)
            .firstOrNull { it.name == member.tier }
            ?.multiplier
            ?: BigDecimal.ONE

    fun evaluate(member: MemberEntity, programId: Long, recognitionPoints: Long): TierEvaluationResult {
        val eligibleTier = tierRepository
            .findByTenantIdAndProgramIdOrderByRank(member.tenantId, programId)
            .filter { recognitionPoints >= it.thresholdPoints }
            .maxByOrNull { it.rank }
            ?: return TierEvaluationResult(member.tier, false)

        val upgraded = member.tier != eligibleTier.name
        if (upgraded) {
            memberRepository.save(member.copy(tier = eligibleTier.name))
        }

        return TierEvaluationResult(eligibleTier.name, upgraded)
    }
}
