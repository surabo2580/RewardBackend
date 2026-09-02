package com.reward.platform.api.controller

import com.reward.platform.api.dto.RewardEventRequest
import com.reward.platform.api.dto.RewardEventResponse
import com.reward.platform.api.entity.AccountEntity
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.entity.BranchRuleEntity
import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.entity.SponsorEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.BranchRepository
import com.reward.platform.api.repository.BranchRuleRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.PartnerMembershipRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api")
class RewardEventController(
    private val accountRepository: AccountRepository,
    private val branchRepository: BranchRepository,
    private val branchRuleRepository: BranchRuleRepository,
    private val memberRepository: MemberRepository,
    private val partnerMembershipRepository: PartnerMembershipRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository
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

        val member = resolveMember(request, sponsor)
            ?: return ResponseEntity.badRequest().body(
                RewardEventResponse(success = false, pointsAwarded = 0, message = "Member not found")
            )

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

        val account = accountRepository.findByTenantIdAndMemberIdAndAccountType(
            tenantId = request.tenantId,
            memberId = member.id,
            accountType = "EARN_REDEEM"
        ) ?: AccountEntity(
            id = 0,
            tenantId = request.tenantId,
            memberId = member.id,
            accountType = "EARN_REDEEM",
            availablePoints = 0,
            pendingPoints = 0,
            redeemedPoints = 0,
            updatedAt = Instant.now()
        )

        val eventType = request.eventType.uppercase()
        val sponsorAncestorIds = collectSponsorAncestorIds(sponsor)
        val configuredRules = branchRuleRepository
            .findByTenantIdAndEventTypeAndIsActiveTrue(request.tenantId, eventType)
            .filter { it.programId == null || it.programId == request.programId }
            .filter {
                it.scope == "PROGRAM" ||
                    (it.scope == "SPONSOR" && it.sponsorId == sponsor.id) ||
                    (it.scope == "LOCATION" && it.locationId == location?.id) ||
                    (it.scope == "PARENT" && it.sponsorId in sponsorAncestorIds) ||
                    (it.scope == "PARTNER" && sponsor.sponsorType == "PARTNER" && it.sponsorId == sponsor.id)
            }
            .filter { it.minAmount == null || BigDecimal.valueOf(request.amount) >= it.minAmount }
            .sortedWith(compareByDescending<BranchRuleEntity> { if (it.scope == "LOCATION") 3 else if (it.scope == "SPONSOR") 2 else 1 }.thenByDescending { it.priority })

        val pointsAwarded = if (configuredRules.isNotEmpty()) {
            configuredRules.sumOf { rule ->
                when (rule.rewardType.uppercase()) {
                    "PERCENTAGE" -> BigDecimal.valueOf(request.amount)
                        .multiply(rule.rewardValue)
                        .divide(BigDecimal(100))
                        .toLong()
                    "MULTIPLIER" -> BigDecimal.valueOf(request.amount)
                        .multiply(rule.rewardValue)
                        .toLong()
                    else -> rule.rewardValue.toLong()
                }
            }
        } else {
            when (eventType) {
                "PURCHASE" -> if (request.amount > 0) request.amount / 10 else 10
                "SIGNUP" -> 100
                "REFERRAL" -> 250
                else -> 0
            }
        }

        val updatedAccount = account.copy(
            availablePoints = account.availablePoints + pointsAwarded,
            updatedAt = Instant.now()
        )
        accountRepository.save(updatedAccount)

        val transaction = TransactionEntity(
            id = 0,
            tenantId = request.tenantId,
            programId = request.programId,
            sponsorId = sponsor.id,
            locationId = location?.id,
            branchId = branch?.id,
            memberId = member.id,
            accountId = updatedAccount.id,
            eventType = eventType,
            transactionType = "EARN",
            amount = request.amount,
            points = pointsAwarded,
            status = "APPROVED",
            referenceId = request.referenceId,
            channel = request.channel?.ifBlank { "POS" } ?: "POS",
            createdAt = Instant.now()
        )
        transactionRepository.save(transaction)

        val walletHistory = WalletHistoryEntity(
            id = 0,
            tenantId = request.tenantId,
            programId = request.programId,
            sponsorId = sponsor.id,
            locationId = location?.id,
            branchId = branch?.id,
            memberId = member.id,
            accountId = updatedAccount.id,
            entryType = "CREDIT",
            points = pointsAwarded,
            description = "Awarded for $eventType${branch?.let { " at ${it.name}" } ?: ""}",
            createdAt = Instant.now()
        )
        walletHistoryRepository.save(walletHistory)

        return ResponseEntity.ok(
            RewardEventResponse(
                success = true,
                pointsAwarded = pointsAwarded,
                message = "Awarded $pointsAwarded points for ${request.eventType}"
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

    private fun collectSponsorAncestorIds(sponsor: SponsorEntity): Set<Long> {
        val ancestors = mutableSetOf<Long>()
        var currentParentId = sponsor.parentSponsorId
        while (currentParentId != null) {
            if (!ancestors.add(currentParentId)) {
                break
            }
            val parent = sponsorRepository.findById(currentParentId).orElse(null) ?: break
            currentParentId = parent.parentSponsorId
        }
        return ancestors
    }
}