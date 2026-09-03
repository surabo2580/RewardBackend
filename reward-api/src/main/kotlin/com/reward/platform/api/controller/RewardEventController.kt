package com.reward.platform.api.controller

import com.reward.platform.api.dto.RewardEventRequest
import com.reward.platform.api.dto.RewardEventResponse
import com.reward.platform.api.entity.AccountEntity
import com.reward.platform.api.entity.OfferApplicationEntity
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.entity.SponsorEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.BranchRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.OfferApplicationRepository
import com.reward.platform.api.repository.PartnerMembershipRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.service.RewardPolicyResolver
import com.reward.platform.api.service.OfferEvaluationService
import com.reward.platform.api.service.PointExpiryPolicyService
import com.reward.platform.api.service.TierEvaluationService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api")
class RewardEventController(
    private val accountRepository: AccountRepository,
    private val branchRepository: BranchRepository,
    private val memberRepository: MemberRepository,
    private val offerApplicationRepository: OfferApplicationRepository,
    private val partnerMembershipRepository: PartnerMembershipRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository,
    private val offerEvaluationService: OfferEvaluationService,
    private val pointExpiryPolicyService: PointExpiryPolicyService,
    private val rewardPolicyResolver: RewardPolicyResolver,
    private val tierEvaluationService: TierEvaluationService
) {
    companion object {
        private const val DEFAULT_BRANCH_CODE = "DEFAULT_MAIN"
        private const val DEFAULT_LOCATION_CODE = "ONLINE_DEFAULT"
    }

    @PostMapping("/events")
    @Transactional
    fun processEvent(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: RewardEventRequest
    ): ResponseEntity<RewardEventResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }

        val program = programRepository.findById(request.programId).orElse(null)
        require(program != null && program.tenantId == request.tenantId) { "Program does not belong to tenant" }

        val sponsor = when {
            request.sponsorId != null -> sponsorRepository.findById(request.sponsorId).orElse(null)
            request.sponsorCode != null -> sponsorRepository.findByTenantIdAndProgramIdAndSponsorCode(
                request.tenantId, request.programId, request.sponsorCode
            )
            else -> null
        }
        require(sponsor != null && sponsor.tenantId == request.tenantId && sponsor.programId == request.programId) {
            "Sponsor is required and must belong to program"
        }

        val resolvedMember = resolveMember(request, sponsor)
            ?: return ResponseEntity.badRequest().body(
                RewardEventResponse(success = false, pointsAwarded = 0, message = "Member not found")
            )
        val member = memberRepository.findLockedByIdAndTenantId(resolvedMember.id, request.tenantId)
            ?: return ResponseEntity.badRequest().body(
                RewardEventResponse(success = false, pointsAwarded = 0, message = "Member not found")
            )

        val referenceId = request.referenceId?.trim().orEmpty()
        if (referenceId.isNotEmpty()) {
            val existing = transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(
                request.tenantId,
                referenceId,
                "EARN"
            )
            if (existing != null) {
                return ResponseEntity.ok(
                    RewardEventResponse(
                        success = true,
                        pointsAwarded = existing.points,
                        recognitionPointsAwarded = existing.recognitionPoints,
                        policyId = existing.policyId,
                        policyScope = existing.policyScope,
                        currentTier = member.tier,
                        message = "Transaction reference '$referenceId' was already processed"
                    )
                )
            }
        }

        val requestedLocation = when {
            request.locationId != null -> locationRepository.findById(request.locationId).orElse(null)
            request.locationCode != null -> locationRepository.findByTenantIdAndLocationCode(request.tenantId, request.locationCode)
            else -> null
        }
        require(requestedLocation == null || (requestedLocation.tenantId == request.tenantId && requestedLocation.sponsorId == sponsor.id)) {
            "Location must belong to sponsor"
        }

        val location = requestedLocation ?: locationRepository.findByTenantIdAndSponsorIdAndLocationCode(
            request.tenantId,
            sponsor.id,
            DEFAULT_LOCATION_CODE
        )
            ?: return ResponseEntity.badRequest().body(
                RewardEventResponse(
                    success = false,
                    pointsAwarded = 0,
                    message = "Cannot process transaction event. Configure default location '$DEFAULT_LOCATION_CODE' for this sponsor or provide a valid locationId/locationCode."
                )
            )

        val branchCode = request.branchCode?.trim().orEmpty()
        val branch = if (branchCode.isNotEmpty()) {
            branchRepository.findByTenantIdAndCode(request.tenantId, branchCode)
                ?: return ResponseEntity.badRequest().body(
                    RewardEventResponse(
                        success = false,
                        pointsAwarded = 0,
                        message = "Branch '$branchCode' not found for this tenant"
                    )
                )
        } else {
            branchRepository.findByTenantIdAndCodeAndStatus(request.tenantId, DEFAULT_BRANCH_CODE, "ACTIVE")
                ?: return ResponseEntity.badRequest().body(
                    RewardEventResponse(
                        success = false,
                        pointsAwarded = 0,
                        message = "Cannot process transaction event. Configure default branch '$DEFAULT_BRANCH_CODE' or provide branchCode."
                    )
                )
        }

        val eventType = request.eventType.uppercase()
        val earnedPoints = rewardPolicyResolver.resolveEarnPoints(
            tenantId = request.tenantId,
            programId = request.programId,
            sponsorId = sponsor.id,
            locationId = location.id,
            eventType = eventType,
            amount = request.amount
        )
        val tierMultiplier = tierEvaluationService.currentMultiplier(member, request.programId)
        val offerResult = if (eventType == "PURCHASE") {
            offerEvaluationService.evaluate(
                tenantId = request.tenantId,
                programId = request.programId,
                memberId = member.id,
                sponsorId = sponsor.id,
                locationId = location.id,
                tierRank = tierEvaluationService.currentRank(member, request.programId),
                amount = request.amount
            )
        } else {
            com.reward.platform.api.service.OfferEvaluationResult()
        }
        val redemptionPoints = java.math.BigDecimal.valueOf(earnedPoints.redemption)
            .multiply(tierMultiplier)
            .multiply(offerResult.multiplier)
            .add(java.math.BigDecimal.valueOf(offerResult.bonusPoints))
            .setScale(0, RoundingMode.DOWN)
            .longValueExact()
        val recognitionPoints = java.math.BigDecimal.valueOf(earnedPoints.recognition)
            .multiply(offerResult.multiplier)
            .add(java.math.BigDecimal.valueOf(offerResult.bonusPoints))
            .setScale(0, RoundingMode.DOWN)
            .longValueExact()

        val redemptionAccount = accountRepository.findByTenantIdAndMemberIdAndAccountType(
            tenantId = request.tenantId,
            memberId = member.id,
            accountType = "REDEMPTION"
        ) ?: AccountEntity(
            id = 0,
            tenantId = request.tenantId,
            memberId = member.id,
            accountType = "REDEMPTION",
            availablePoints = 0,
            pendingPoints = 0,
            redeemedPoints = 0,
            lifetimeEarnedPoints = 0,
            updatedAt = Instant.now()
        )
        val updatedRedemptionAccount = accountRepository.save(redemptionAccount.copy(
            availablePoints = redemptionAccount.availablePoints + redemptionPoints,
            lifetimeEarnedPoints = redemptionAccount.lifetimeEarnedPoints + redemptionPoints,
            updatedAt = Instant.now()
        ))

        val recognitionAccount = accountRepository.findByTenantIdAndMemberIdAndAccountType(
            tenantId = tenantId,
            memberId = member.id,
            accountType = "RECOGNITION"
        ) ?: AccountEntity(
            tenantId = tenantId,
            memberId = member.id,
            accountType = "RECOGNITION"
        )
        val updatedRecognitionAccount = accountRepository.save(recognitionAccount.copy(
            availablePoints = recognitionAccount.availablePoints + recognitionPoints,
            lifetimeEarnedPoints = recognitionAccount.lifetimeEarnedPoints + recognitionPoints,
            updatedAt = Instant.now()
        ))

        val tierResult = tierEvaluationService.evaluate(member, request.programId, updatedRecognitionAccount.lifetimeEarnedPoints)

        val transaction = TransactionEntity(
            id = 0,
            tenantId = request.tenantId,
            programId = request.programId,
            sponsorId = sponsor.id,
            locationId = location?.id,
            branchId = branch?.id,
            memberId = member.id,
            accountId = updatedRedemptionAccount.id,
            eventType = eventType,
            transactionType = "EARN",
            amount = request.amount,
            points = redemptionPoints,
            recognitionPoints = recognitionPoints,
            policyId = earnedPoints.rule?.id,
            policyScope = earnedPoints.rule?.scope,
            offerMultiplier = offerResult.multiplier,
            offerBonusPoints = offerResult.bonusPoints,
            status = "APPROVED",
            referenceId = referenceId.ifBlank { null },
            channel = request.channel?.ifBlank { "POS" } ?: "POS",
            createdAt = Instant.now()
        )
        val savedTransaction = transactionRepository.save(transaction)
        offerResult.offerIds.forEach { offerId ->
            offerApplicationRepository.save(
                OfferApplicationEntity(
                    tenantId = request.tenantId,
                    offerId = offerId,
                    memberId = member.id,
                    transactionId = savedTransaction.id
                )
            )
        }

        val earnedAt = Instant.now()
        val redemptionLedgerEntry = WalletHistoryEntity(
            id = 0,
            tenantId = request.tenantId,
            programId = request.programId,
            sponsorId = sponsor.id,
            locationId = location?.id,
            branchId = branch?.id,
            memberId = member.id,
            accountId = updatedRedemptionAccount.id,
            accountType = "REDEMPTION",
            entryType = "CREDIT",
            points = redemptionPoints,
            policyId = earnedPoints.rule?.id,
            policyScope = earnedPoints.rule?.scope,
            description = "REDEMPTION credit for $eventType${earnedPoints.rule?.let { " using ${it.scope} policy '${it.name}'" } ?: " using default policy"}${offerResult.offerIds.takeIf { it.isNotEmpty() }?.let { " with offers ${it.joinToString()}" } ?: ""}",
            expiresAt = pointExpiryPolicyService.expiresAt(program, earnedAt),
            remainingPoints = redemptionPoints,
            createdAt = earnedAt
        )
        walletHistoryRepository.save(redemptionLedgerEntry)

        walletHistoryRepository.save(
            WalletHistoryEntity(
                tenantId = request.tenantId,
                programId = request.programId,
                sponsorId = sponsor.id,
                locationId = location.id,
                branchId = branch.id,
                memberId = member.id,
                accountId = updatedRecognitionAccount.id,
                accountType = "RECOGNITION",
                entryType = "CREDIT",
                points = recognitionPoints,
                policyId = earnedPoints.rule?.id,
                policyScope = earnedPoints.rule?.scope,
                description = "RECOGNITION credit for $eventType${earnedPoints.rule?.let { " using ${it.scope} policy '${it.name}'" } ?: " using default policy"}${offerResult.offerIds.takeIf { it.isNotEmpty() }?.let { " with offers ${it.joinToString()}" } ?: ""}"
            )
        )

        return ResponseEntity.ok(
            RewardEventResponse(
                success = true,
                pointsAwarded = redemptionPoints,
                recognitionPointsAwarded = recognitionPoints,
                policyId = earnedPoints.rule?.id,
                policyScope = earnedPoints.rule?.scope,
                appliedOfferIds = offerResult.offerIds,
                currentTier = tierResult.currentTier,
                tierUpgraded = tierResult.upgraded,
                message = "Awarded $redemptionPoints redemption and ${earnedPoints.recognition} recognition points for ${request.eventType}"
            )
        )
    }

    private fun resolveMember(request: RewardEventRequest, sponsor: SponsorEntity): MemberEntity? {
        val directMemberId = request.memberId?.trim().orEmpty()
        if (directMemberId.isNotEmpty()) {
            return memberRepository.findByTenantIdAndExternalUserId(request.tenantId, directMemberId)
        }

        val externalMembershipId = request.externalMembershipId?.trim().orEmpty()
        if (externalMembershipId.isEmpty() || sponsor.sponsorType != "PARTNER") {
            return null
        }

        val partnerMembership = partnerMembershipRepository.findByTenantIdAndSponsorIdAndExternalMembershipId(
            request.tenantId,
            sponsor.id,
            externalMembershipId
        ) ?: return null

        return memberRepository.findById(partnerMembership.memberId).orElse(null)
            ?.takeIf { it.tenantId == request.tenantId }
    }

}