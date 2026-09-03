package com.reward.platform.api.controller

import com.reward.platform.api.dto.DealRedemptionRequest
import com.reward.platform.api.dto.DealRedemptionResponse
import com.reward.platform.api.dto.PrivilegeClaimRequest
import com.reward.platform.api.dto.PrivilegeClaimResponse
import com.reward.platform.api.dto.RewardClaimRequest
import com.reward.platform.api.dto.RewardClaimResponse
import com.reward.platform.api.entity.OfferApplicationEntity
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.OfferApplicationRepository
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.OfferTargetMemberRepository
import com.reward.platform.api.repository.OfferVoucherRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.TierRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import com.reward.platform.api.service.RedemptionLotService
import com.reward.platform.api.service.OfferCsvImportService
import com.reward.platform.api.service.OfferImportSummary
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/offers")
class OfferExecutionController(
    private val accountRepository: AccountRepository,
    private val memberRepository: MemberRepository,
    private val offerApplicationRepository: OfferApplicationRepository,
    private val offerRepository: OfferRepository,
    private val offerTargetMemberRepository: OfferTargetMemberRepository,
    private val offerVoucherRepository: OfferVoucherRepository,
    private val offerCsvImportService: OfferCsvImportService,
    private val redemptionLotService: RedemptionLotService,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository,
    private val tierRepository: TierRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository
) {
    @PostMapping("/import-campaigns", consumes = ["multipart/form-data"])
    fun importCampaigns(@RequestAttribute("tenantId") tenantId: Long, @RequestParam programId: Long, @RequestParam file: MultipartFile): ResponseEntity<OfferImportSummary> =
        ResponseEntity.ok(offerCsvImportService.importCampaigns(tenantId, programId, file.inputStream))

    @PostMapping("/import-vouchers", consumes = ["multipart/form-data"])
    fun importVouchers(@RequestAttribute("tenantId") tenantId: Long, @RequestParam file: MultipartFile): ResponseEntity<OfferImportSummary> =
        ResponseEntity.ok(offerCsvImportService.importVouchers(tenantId, file.inputStream))

    @PostMapping("/{offerId}/claim-reward")
    @Transactional
    fun claimReward(@RequestAttribute("tenantId") tenantId: Long, @PathVariable offerId: Long, @Valid @RequestBody request: RewardClaimRequest): ResponseEntity<RewardClaimResponse> {
        val reference = request.referenceId.trim()
        transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(tenantId, reference, "REWARD_CLAIM")?.let { existing ->
            return ResponseEntity.ok(RewardClaimResponse(true, "ALREADY_PROCESSED", existing.id, "Reward claim", existing.points, offerVoucherRepository.findByTenantIdAndReferenceId(tenantId, reference)?.voucherCode, 0, "This claim reference was already processed"))
        }
        val offer = eligibleOffer(tenantId, offerId, "REWARD")
        val member = memberRepository.findByTenantIdAndExternalUserId(tenantId, request.memberId.trim()) ?: return ResponseEntity.badRequest().body(rewardError("MEMBER_NOT_FOUND", "Member not found"))
        validateMemberEligibility(offer.id, offer, member.id, tenantId)
        val account = accountRepository.findLockedByTenantIdAndMemberIdAndAccountType(tenantId, member.id, "REDEMPTION") ?: return ResponseEntity.badRequest().body(rewardError("ACCOUNT_NOT_FOUND", "Member has no redemption account"))
        if (account.availablePoints < offer.pointsRequired || !redemptionLotService.consume(tenantId, member.id, offer.pointsRequired)) {
            return ResponseEntity.badRequest().body(rewardError("INSUFFICIENT_BALANCE", "Insufficient unexpired redemption points"))
        }
        val voucher = offerVoucherRepository.findFirstByTenantIdAndOfferIdAndIsIssuedFalse(tenantId, offer.id)
        val updatedAccount = accountRepository.save(account.copy(availablePoints = account.availablePoints - offer.pointsRequired, redeemedPoints = account.redeemedPoints + offer.pointsRequired, updatedAt = Instant.now()))
        val transaction = transactionRepository.save(TransactionEntity(tenantId = tenantId, programId = offer.programId, memberId = member.id, accountId = updatedAccount.id, eventType = "REWARD_CLAIM", transactionType = "REWARD_CLAIM", points = offer.pointsRequired, policyId = offer.id, referenceId = reference, channel = "MEMBER_CLAIM"))
        voucher?.let { offerVoucherRepository.save(it.copy(isIssued = true, issuedToMemberId = member.id, issuedAt = Instant.now(), referenceId = reference, expiresAt = offer.endDate)) }
        walletHistoryRepository.save(WalletHistoryEntity(tenantId = tenantId, programId = offer.programId, memberId = member.id, accountId = updatedAccount.id, accountType = "REDEMPTION", entryType = "DEBIT", points = offer.pointsRequired, policyId = offer.id, description = "Reward claimed: ${offer.name}"))
        recordClaim(tenantId, offer, member.id, transaction.id)
        return ResponseEntity.ok(RewardClaimResponse(true, "SUCCESS", transaction.id, offer.name, offer.pointsRequired, voucher?.voucherCode, updatedAccount.availablePoints, "Reward claimed successfully"))
    }

    @PostMapping("/{offerId}/claim-privilege")
    @Transactional
    fun claimPrivilege(@RequestAttribute("tenantId") tenantId: Long, @PathVariable offerId: Long, @Valid @RequestBody request: PrivilegeClaimRequest): ResponseEntity<PrivilegeClaimResponse> {
        val reference = request.referenceId.trim()
        val member = memberRepository.findByTenantIdAndExternalUserId(tenantId, request.memberId.trim()) ?: return ResponseEntity.badRequest().body(PrivilegeClaimResponse(false, "MEMBER_NOT_FOUND", offerName = "", previousTier = "", currentTier = "", message = "Member not found"))
        transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(tenantId, reference, "PRIVILEGE_CLAIM")?.let { existing -> return ResponseEntity.ok(PrivilegeClaimResponse(true, "ALREADY_PROCESSED", existing.id, "Privilege claim", member.tier, member.tier, message = "This claim reference was already processed")) }
        val offer = eligibleOffer(tenantId, offerId, "PRIVILEGE")
        validateMemberEligibility(offer.id, offer, member.id, tenantId)
        val lockedMember = memberRepository.findLockedByIdAndTenantId(member.id, tenantId) ?: error("Member not found")
        val previousTier = lockedMember.tier
        val targetTier = offer.targetTierId?.let { tierRepository.findById(it).orElse(null)?.takeIf { tier -> tier.tenantId == tenantId && tier.programId == offer.programId } }
        val currentRank = tierRepository.findByTenantIdAndProgramIdOrderByRank(tenantId, offer.programId).firstOrNull { it.name == lockedMember.tier }?.rank ?: 0
        val updatedMember = if (targetTier != null && targetTier.rank > currentRank) memberRepository.save(lockedMember.copy(tier = targetTier.name)) else lockedMember
        val transaction = transactionRepository.save(TransactionEntity(tenantId = tenantId, programId = offer.programId, memberId = member.id, accountId = 0, eventType = "PRIVILEGE", transactionType = "PRIVILEGE_CLAIM", policyId = offer.id, referenceId = reference, channel = "MEMBER_CLAIM"))
        walletHistoryRepository.save(WalletHistoryEntity(tenantId = tenantId, programId = offer.programId, memberId = member.id, accountId = 0, accountType = "RECOGNITION", entryType = "CREDIT", points = 0, policyId = offer.id, description = "Privilege activated: ${offer.benefitCode ?: offer.name}"))
        recordClaim(tenantId, offer, member.id, transaction.id)
        return ResponseEntity.ok(PrivilegeClaimResponse(true, "SUCCESS", transaction.id, offer.name, previousTier, updatedMember.tier, offer.benefitCode, "Privilege activated"))
    }

    @PostMapping("/redeem-deal")
    @Transactional
    fun redeemDeal(@RequestAttribute("tenantId") authenticatedTenantId: Long, @Valid @RequestBody request: DealRedemptionRequest): ResponseEntity<DealRedemptionResponse> {
        require(request.tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        val reference = request.referenceId.trim()
        transactionRepository.findByTenantIdAndReferenceIdAndTransactionType(request.tenantId, reference, "DEAL_DISCOUNT")?.let { existing -> return ResponseEntity.ok(DealRedemptionResponse(true, "ALREADY_PROCESSED", existing.id, request.offerCode, existing.amount, existing.discountAmount?.toPlainString() ?: "0", "0", "This deal reference was already processed")) }
        val offer = offerRepository.findByTenantIdAndOfferCode(request.tenantId, request.offerCode.trim().uppercase()) ?: throw IllegalArgumentException("Deal offer not found")
        require(offer.category == "DEAL") { "Offer is not a Deal" }
        require(offer.programId == request.programId) { "Offer does not belong to program" }
        validateActive(offer)
        val member = memberRepository.findByTenantIdAndExternalUserId(request.tenantId, request.memberId.trim()) ?: return ResponseEntity.badRequest().body(DealRedemptionResponse(false, "MEMBER_NOT_FOUND", offerCode = offer.offerCode, originalAmount = request.billAmount, discountAmount = "0", netPayableAmount = request.billAmount.toString(), message = "Member not found"))
        validateMemberEligibility(offer.id, offer, member.id, request.tenantId)
        val sponsor = request.sponsorId?.let { sponsorRepository.findById(it).orElse(null) }
        if (offer.scope != "PROGRAM") require(sponsor != null && sponsor.tenantId == request.tenantId) { "A valid sponsor is required for this deal" }
        request.locationId?.let { locationId -> require(locationRepository.findById(locationId).orElse(null)?.sponsorId == sponsor?.id) { "Location must belong to sponsor" } }
        require(request.billAmount >= offer.minSpend.toLong()) { "Bill amount is below the minimum spend" }
        val rate = offer.discountValue ?: BigDecimal.ZERO
        val discount = if (offer.discountType == "PERCENTAGE") BigDecimal.valueOf(request.billAmount).multiply(rate).divide(BigDecimal(100), 2, RoundingMode.HALF_UP) else rate.min(BigDecimal.valueOf(request.billAmount))
        val net = BigDecimal.valueOf(request.billAmount).subtract(discount).max(BigDecimal.ZERO)
        val transaction = transactionRepository.save(TransactionEntity(tenantId = request.tenantId, programId = offer.programId, sponsorId = sponsor?.id, locationId = request.locationId, memberId = member.id, accountId = 0, eventType = "DEAL", transactionType = "DEAL_DISCOUNT", amount = request.billAmount, discountAmount = discount, policyId = offer.id, referenceId = reference, channel = "POS"))
        recordClaim(request.tenantId, offer, member.id, transaction.id)
        return ResponseEntity.ok(DealRedemptionResponse(true, "APPLIED", transaction.id, offer.offerCode, request.billAmount, discount.toPlainString(), net.toPlainString(), "Deal applied"))
    }

    private fun eligibleOffer(tenantId: Long, offerId: Long, category: String) = offerRepository.findByTenantIdAndId(tenantId, offerId)?.also { offer -> require(offer.category == category) { "Offer is not a $category campaign" }; validateActive(offer) } ?: throw IllegalArgumentException("Offer not found")
    private fun validateActive(offer: com.reward.platform.api.entity.OfferEntity) { val now = Instant.now(); require(offer.status == "LAUNCHED" && offer.isActive && !now.isBefore(offer.startDate) && !now.isAfter(offer.endDate)) { "Offer is not active" } }
    private fun validateMemberEligibility(offerId: Long, offer: com.reward.platform.api.entity.OfferEntity, memberId: Long, tenantId: Long) { require(!offer.isMto || offerTargetMemberRepository.existsByOfferIdAndMemberId(offerId, memberId)) { "Member is not targeted for this offer" }; require(offer.maxUsesPerMember == null || offerApplicationRepository.countByTenantIdAndMemberIdAndOfferId(tenantId, memberId, offerId) < offer.maxUsesPerMember) { "Member claim limit reached" }; require(offer.maxTotalClaims == null || offer.totalClaimsCount < offer.maxTotalClaims) { "Offer claim limit reached" } }
    private fun recordClaim(tenantId: Long, offer: com.reward.platform.api.entity.OfferEntity, memberId: Long, transactionId: Long) { offerApplicationRepository.save(OfferApplicationEntity(tenantId = tenantId, offerId = offer.id, memberId = memberId, transactionId = transactionId)); offerRepository.save(offer.copy(totalClaimsCount = offer.totalClaimsCount + 1)) }
    private fun rewardError(status: String, message: String) = RewardClaimResponse(false, status, offerName = "", pointsBurned = 0, remainingBalance = 0, message = message)
}
