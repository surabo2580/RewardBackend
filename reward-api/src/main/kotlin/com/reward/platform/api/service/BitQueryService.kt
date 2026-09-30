package com.reward.platform.api.service

import com.reward.platform.api.dto.BitCreateRequest
import com.reward.platform.api.dto.BitDayCount
import com.reward.platform.api.dto.BitDetailResponse
import com.reward.platform.api.dto.BitLedgerEntry
import com.reward.platform.api.dto.BitListRowResponse
import com.reward.platform.api.dto.BitPageResponse
import com.reward.platform.api.dto.BitOfferSummary
import com.reward.platform.api.dto.BitResponse
import com.reward.platform.api.dto.BitVoucherSummary
import com.reward.platform.api.dto.BitSponsorCount
import com.reward.platform.api.dto.MemberBitSummaryResponse
import com.reward.platform.api.entity.BitCategory
import com.reward.platform.api.entity.BitEntity
import com.reward.platform.api.entity.BitType
import com.reward.platform.api.repository.BitRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.OfferVoucherRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.TransactionRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class BitFilter(
    val memberId: Long? = null,
    val bitType: String? = null,
    val category: String? = null,
    val sponsorId: Long? = null,
    val status: String? = null,
    val source: String? = null,
    val pointsAction: String? = null,
    val from: Instant? = null,
    val to: Instant? = null
)

@Service
class BitQueryService(
    private val bitRepository: BitRepository,
    private val bitService: BitService,
    private val memberRepository: MemberRepository,
    private val programRepository: ProgramRepository,
    private val offerRepository: OfferRepository,
    private val offerVoucherRepository: OfferVoucherRepository,
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository,
    private val transactionRepository: TransactionRepository
) {
    private companion object {
        // Types with ledger effects must go through their dedicated endpoints.
        val DIRECT_TYPES = setOf(
            BitType.APP_LOGIN, BitType.SURVEY, BitType.REFERRAL, BitType.SOCIAL_SHARE,
            BitType.EVENT, BitType.PROMO_OFFERS, BitType.HOTEL_STAY, BitType.FLIGHT_SEGMENT,
            BitType.CAR_RENTAL, BitType.PROFILE_UPDATE
        )
    }

    @Transactional
    fun recordDirect(tenantId: Long, request: BitCreateRequest): BitResponse {
        val type = BitType.parse(request.bitType)
        require(type in DIRECT_TYPES) {
            "BIT type ${type.name} affects balances; use its dedicated endpoint (e.g. /api/events, /api/transactions/redeem)"
        }
        val member = memberRepository.findByTenantIdAndExternalUserId(tenantId, request.memberId.trim())
            ?: throw NoSuchElementException("Member not found")
        val program = request.programId?.let { programId ->
            programRepository.findById(programId).orElse(null)?.takeIf { it.tenantId == tenantId }
                ?: throw IllegalArgumentException("Program does not belong to tenant")
        }
        val sponsor = request.sponsorId?.let { sponsorId ->
            sponsorRepository.findById(sponsorId).orElse(null)?.takeIf { it.tenantId == tenantId }
                ?: throw IllegalArgumentException("Sponsor does not belong to tenant")
        }
        require(program == null || sponsor == null || sponsor.programId == program.id) {
            "Sponsor does not belong to program"
        }
        request.locationId?.let { locationId ->
            val location = locationRepository.findById(locationId).orElse(null)
            require(location != null && location.tenantId == tenantId && (sponsor == null || location.sponsorId == sponsor.id)) {
                "Location must belong to tenant and sponsor"
            }
        }
        val now = Instant.now()
        val interactionAt = request.interactionAt ?: now
        require(!interactionAt.isAfter(now)) { "BIT interaction time cannot be in the future" }
        val bit = bitService.record(
            BitCommand(
                tenantId = tenantId,
                memberId = member.id,
                bitType = type,
                reference = request.referenceId.trim(),
                programId = program?.id ?: sponsor?.programId,
                bitSponsorId = sponsor?.id,
                locationId = request.locationId,
                channel = request.channel,
                grossAmount = request.amount,
                currency = request.currency,
                description = request.description,
                payload = request.payload,
                interactionAt = interactionAt
            )
        )
        return toResponses(tenantId, listOf(bit)).first()
    }

    @Transactional(readOnly = true)
    fun list(tenantId: Long, filter: BitFilter, page: Int, size: Int): BitPageResponse {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by(Sort.Direction.DESC, "interactionAt"))
        val result = bitRepository.findAll(specification(tenantId, filter), pageable)
        return BitPageResponse(toListRows(tenantId, result.content), result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional(readOnly = true)
    fun detail(tenantId: Long, bitId: Long): BitDetailResponse {
        val bit = bitRepository.findByTenantIdAndId(tenantId, bitId) ?: throw NoSuchElementException("BIT $bitId not found")
        val ledger = transactionRepository.findByTenantIdAndBitIdOrderByCreatedAtAsc(tenantId, bitId).map {
            BitLedgerEntry(it.id, it.transactionType, it.points, it.recognitionPoints, it.status, it.createdAt)
        }
        val reversals = bitRepository.findByTenantIdAndOriginalBitId(tenantId, bitId)
        val responses = toResponses(tenantId, listOf(bit) + reversals)
        val offers = offerRepository.findAllById(bit.appliedOfferIds?.split(',')?.mapNotNull { it.trim().toLongOrNull() }.orEmpty())
            .filter { it.tenantId == tenantId }
            .map { BitOfferSummary(it.id, it.offerCode, it.name, it.category) }
        val vouchers = offerVoucherRepository.findByTenantIdAndReferenceId(tenantId, bit.bitReference)
            ?.let { listOf(BitVoucherSummary(it.id, it.offerId, it.voucherCode, if (it.isIssued) "ISSUED" else "AVAILABLE", it.expiresAt)) }
            .orEmpty()
        return BitDetailResponse(responses.first(), ledger, responses.drop(1), offers, vouchers)
    }

    @Transactional(readOnly = true)
    fun memberBits(tenantId: Long, memberId: Long, limit: Int): List<BitResponse> {
        requireMember(tenantId, memberId)
        val bits = bitRepository.findByTenantIdAndMemberIdOrderByInteractionAtDesc(tenantId, memberId, PageRequest.of(0, limit.coerceIn(1, 500)))
        return toResponses(tenantId, bits)
    }

    @Transactional(readOnly = true)
    fun memberSummary(tenantId: Long, memberId: Long): MemberBitSummaryResponse {
        requireMember(tenantId, memberId)
        val bits = bitRepository.findByTenantIdAndMemberIdOrderByInteractionAtDesc(tenantId, memberId)
        val names = sponsorNames(tenantId, bits.flatMap { listOfNotNull(it.bitSponsorId, it.billingSponsorId) })
        val lastBitAt = bits.firstOrNull()?.interactionAt
        return MemberBitSummaryResponse(
            totalBits = bits.size.toLong(),
            firstBitAt = bits.lastOrNull()?.interactionAt,
            lastBitAt = lastBitAt,
            daysSinceLastBit = lastBitAt?.let { ChronoUnit.DAYS.between(it, Instant.now()) },
            lastBits = bits.take(5).map { toResponse(it, names) },
            countsByType = bits.groupingBy { it.bitType }.eachCount().mapValues { it.value.toLong() },
            countsByCategory = bits.groupingBy { it.bitCategory }.eachCount().mapValues { it.value.toLong() },
            topSponsors = bits.groupingBy { it.bitSponsorId }.eachCount()
                .entries.sortedByDescending { it.value }.take(5)
                .map { BitSponsorCount(it.key, it.key?.let(names::get), it.value.toLong()) },
            span = bits.groupingBy { it.interactionAt.atZone(ZoneOffset.UTC).toLocalDate() }.eachCount()
                .entries.sortedBy { it.key }
                .map { BitDayCount(it.key, it.value.toLong()) }
        )
    }

    private fun specification(tenantId: Long, filter: BitFilter) = Specification<BitEntity> { root, _, cb ->
        val predicates = mutableListOf<Predicate>(cb.equal(root.get<Long>("tenantId"), tenantId))
        filter.memberId?.let { predicates += cb.equal(root.get<Long>("memberId"), it) }
        filter.bitType?.let { value ->
            val types = parseValues(value).map { BitType.parse(it).name }
            predicates += cb.or(*types.map { cb.equal(root.get<String>("bitType"), it) }.toTypedArray())
        }
        filter.category?.let { value ->
            val categories = parseValues(value).map { categoryValue ->
                BitCategory.entries.firstOrNull { it.name == categoryValue.uppercase() }
                    ?: throw IllegalArgumentException("Unsupported BIT category '$categoryValue'")
            }
            predicates += cb.or(*categories.map { cb.equal(root.get<String>("bitCategory"), it.name) }.toTypedArray())
        }
        filter.sponsorId?.let { predicates += cb.equal(root.get<Long>("bitSponsorId"), it) }
        filter.status?.let { value ->
            val statuses = parseValues(value).map { it.uppercase() }
            val allowedStatuses = setOf("COMPLETED", "PENDING", "FAILED", "ON_HOLD", "REVERSED", "PARTIALLY_REVERSED", "REJECTED", "PROCESSED")
            require(statuses.all { it in allowedStatuses }) { "Unsupported BIT status '$value'" }
            predicates += cb.or(*statuses.map { cb.equal(root.get<String>("status"), it) }.toTypedArray())
        }
        filter.source?.let { value ->
            val sources = parseValues(value).map { it.trim().uppercase() }
            predicates += cb.or(*sources.map { source ->
                cb.or(cb.equal(root.get<String>("bitSource"), source), cb.equal(root.get<String>("channel"), source))
            }.toTypedArray())
        }
        filter.pointsAction?.let { value ->
            val actions = parseValues(value).map { it.lowercase() }
            require(actions.all { it in setOf("rewarded", "redeemed", "expired") }) { "Unsupported points action '$value'" }
            val actionPredicates = actions.map { action ->
                when (action) {
                    "rewarded" -> cb.or(
                        cb.greaterThan(root.get<Long>("redemptionPointsDelta"), 0L),
                        cb.greaterThan(root.get<Long>("recognitionPointsDelta"), 0L)
                    )
                    "redeemed" -> cb.and(
                        cb.or(
                            cb.equal(root.get<String>("bitCategory"), BitCategory.REDEMPTION.name),
                            cb.equal(root.get<String>("bitCategory"), BitCategory.PRIVILEGE.name)
                        ),
                        cb.or(
                            cb.lessThan(root.get<Long>("redemptionPointsDelta"), 0L),
                            cb.lessThan(root.get<Long>("recognitionPointsDelta"), 0L)
                        )
                    )
                    else -> cb.equal(root.get<String>("bitCategory"), BitCategory.EXPIRATION.name)
                }
            }
            predicates += cb.or(*actionPredicates.toTypedArray())
        }
        filter.from?.let { predicates += cb.greaterThanOrEqualTo(root.get("interactionAt"), it) }
        filter.to?.let { predicates += cb.lessThan(root.get("interactionAt"), it) }
        cb.and(*predicates.toTypedArray())
    }

    private fun requireMember(tenantId: Long, memberId: Long) {
        memberRepository.findByIdAndTenantId(memberId, tenantId) ?: throw NoSuchElementException("Member $memberId not found")
    }

    private fun sponsorNames(tenantId: Long, ids: Collection<Long>): Map<Long, String> =
        if (ids.isEmpty()) emptyMap()
        else sponsorRepository.findAllById(ids.toSet()).filter { it.tenantId == tenantId }.associate { it.id to it.name }

    private fun toResponses(tenantId: Long, bits: List<BitEntity>): List<BitResponse> {
        val names = sponsorNames(tenantId, bits.flatMap { listOfNotNull(it.bitSponsorId, it.billingSponsorId) })
        return bits.map { toResponse(it, names) }
    }

    private fun toListRows(tenantId: Long, bits: List<BitEntity>): List<BitListRowResponse> {
        if (bits.isEmpty()) return emptyList()
        val memberNames = memberRepository.findAllById(bits.map { it.memberId }.toSet())
            .filter { it.tenantId == tenantId }
            .associateBy { it.id }
        val sponsors = sponsorNames(tenantId, bits.mapNotNull { it.bitSponsorId })
        val offerIds = bits.flatMap { bit -> bit.appliedOfferIds?.split(',')?.mapNotNull { it.trim().toLongOrNull() }.orEmpty() }.toSet()
        val offers = offerRepository.findAllById(offerIds).filter { it.tenantId == tenantId }.associateBy { it.id }
        val issuedVoucherCounts = offerVoucherRepository.findByTenantIdAndReferenceIdIn(tenantId, bits.map { it.bitReference }.distinct())
            .filter { it.isIssued }
            .groupingBy { it.referenceId }
            .eachCount()
        return bits.map { bit ->
            val firstOffer = bit.appliedOfferIds?.split(',')?.firstNotNullOfOrNull { offers[it.trim().toLongOrNull()] }
            val pointsDelta = bit.redemptionPointsDelta + bit.recognitionPointsDelta
            BitListRowResponse(
                bitId = bit.id,
                bitReference = bit.bitReference,
                interactionDate = bit.interactionAt,
                sponsorName = bit.bitSponsorId?.let(sponsors::get),
                memberCode = memberNames[bit.memberId]?.externalUserId ?: bit.memberId.toString(),
                bitCategory = bit.bitCategory,
                bitType = bit.bitType,
                bitTypeLabel = BitType.entries.firstOrNull { it.name == bit.bitType }?.label ?: bit.bitType,
                offerName = firstOffer?.name,
                pointsDelta = pointsDelta.takeIf { it != 0L },
                redemptionPoints = bit.redemptionPointsDelta,
                recognitionPoints = bit.recognitionPointsDelta,
                rewardsEarned = issuedVoucherCounts[bit.bitReference] ?: 0,
                rewardsAvailed = null,
                status = bit.status,
                errorCode = bit.errorCode,
                errorMessage = bit.errorMessage,
                source = bit.bitSource ?: bit.channel
            )
        }
    }

    private fun parseValues(value: String): List<String> = value.split(',').map(String::trim).filter(String::isNotEmpty).also {
        require(it.isNotEmpty()) { "Filter value must not be blank" }
    }

    private fun toResponse(bit: BitEntity, sponsorNames: Map<Long, String>) = BitResponse(
        id = bit.id,
        bitReference = bit.bitReference,
        bitType = bit.bitType,
        bitTypeLabel = BitType.entries.firstOrNull { it.name == bit.bitType }?.label ?: bit.bitType,
        bitCategory = bit.bitCategory,
        status = bit.status,
        errorCode = bit.errorCode,
        errorMessage = bit.errorMessage,
        memberId = bit.memberId,
        programId = bit.programId,
        bitSponsorId = bit.bitSponsorId,
        bitSponsorName = bit.bitSponsorId?.let(sponsorNames::get),
        billingSponsorId = bit.billingSponsorId,
        billingSponsorName = bit.billingSponsorId?.let(sponsorNames::get),
        locationId = bit.locationId,
        branchId = bit.branchId,
        channel = bit.channel,
        bitSource = bit.bitSource ?: bit.channel,
        grossAmount = bit.grossAmount,
        discountAmount = bit.discountAmount,
        netAmount = bit.netAmount,
        currency = bit.currency,
        redemptionPointsDelta = bit.redemptionPointsDelta,
        recognitionPointsDelta = bit.recognitionPointsDelta,
        appliedPolicyId = bit.appliedPolicyId,
        appliedOfferIds = bit.appliedOfferIds?.split(',')?.mapNotNull { it.trim().toLongOrNull() }.orEmpty(),
        originalBitId = bit.originalBitId,
        description = bit.description,
        payload = bitService.parsePayload(bit),
        createdByUserId = bit.createdByUserId,
        interactionAt = bit.interactionAt,
        createdAt = bit.createdAt
    )
}
