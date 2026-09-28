package com.reward.platform.api.service

import com.reward.platform.api.dto.HotnoteResponse
import com.reward.platform.api.dto.Member360Response
import com.reward.platform.api.dto.MemberBalanceDetailResponse
import com.reward.platform.api.dto.MemberBalancesResponse
import com.reward.platform.api.dto.MemberBookingCreateRequest
import com.reward.platform.api.dto.MemberBookingResponse
import com.reward.platform.api.dto.MemberCountsResponse
import com.reward.platform.api.dto.MemberKpiResponse
import com.reward.platform.api.dto.MemberLinkCreateRequest
import com.reward.platform.api.dto.MemberLinkResponse
import com.reward.platform.api.dto.MemberOfferResponse
import com.reward.platform.api.dto.MemberProfileResponse
import com.reward.platform.api.dto.MemberProfileUpdateRequest
import com.reward.platform.api.dto.MemberSearchResult
import com.reward.platform.api.dto.MemberTransactionResponse
import com.reward.platform.api.dto.MemberVoucherResponse
import com.reward.platform.api.dto.MembershipCardIssueRequest
import com.reward.platform.api.dto.MembershipCardResponse
import com.reward.platform.api.dto.MonthlyActivity
import com.reward.platform.api.dto.PointAdjustmentRequest
import com.reward.platform.api.dto.PointAdjustmentResponse
import com.reward.platform.api.dto.PointLotResponse
import com.reward.platform.api.dto.ServiceTicketCreateRequest
import com.reward.platform.api.dto.ServiceTicketResponse
import com.reward.platform.api.dto.ServiceTicketUpdateRequest
import com.reward.platform.api.dto.TierOverrideRequest
import com.reward.platform.api.dto.TierSummaryResponse
import com.reward.platform.api.entity.AccountEntity
import com.reward.platform.api.entity.MemberBookingEntity
import com.reward.platform.api.entity.MemberEntity
import com.reward.platform.api.entity.MemberLinkEntity
import com.reward.platform.api.entity.MemberServiceTicketEntity
import com.reward.platform.api.entity.MembershipCardEntity
import com.reward.platform.api.entity.OfferEntity
import com.reward.platform.api.entity.ProgramEntity
import com.reward.platform.api.entity.TransactionEntity
import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.AccountRepository
import com.reward.platform.api.repository.MemberBookingRepository
import com.reward.platform.api.repository.MemberLinkRepository
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.MemberServiceTicketRepository
import com.reward.platform.api.repository.MembershipCardRepository
import com.reward.platform.api.repository.OfferApplicationRepository
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.OfferTargetMemberRepository
import com.reward.platform.api.repository.OfferVoucherRepository
import com.reward.platform.api.repository.PartnerMembershipRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorRepository
import com.reward.platform.api.repository.TierRepository
import com.reward.platform.api.repository.TransactionRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class Member360Service(
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val walletHistoryRepository: WalletHistoryRepository,
    private val programRepository: ProgramRepository,
    private val tierRepository: TierRepository,
    private val sponsorRepository: SponsorRepository,
    private val offerRepository: OfferRepository,
    private val offerTargetMemberRepository: OfferTargetMemberRepository,
    private val offerApplicationRepository: OfferApplicationRepository,
    private val offerVoucherRepository: OfferVoucherRepository,
    private val partnerMembershipRepository: PartnerMembershipRepository,
    private val memberLinkRepository: MemberLinkRepository,
    private val membershipCardRepository: MembershipCardRepository,
    private val memberBookingRepository: MemberBookingRepository,
    private val ticketRepository: MemberServiceTicketRepository,
    private val pointExpiryPolicyService: PointExpiryPolicyService
) {
    private val random = SecureRandom()

    private companion object {
        val MEMBER_STATUSES = setOf("ACTIVE", "TEMPORARY", "SUSPENDED", "BLOCKED", "CLOSED")
        val CARD_STATUSES = setOf("ACTIVE", "BLOCKED", "EXPIRED", "REPLACED")
        val BOOKING_STATUSES = setOf("CONFIRMED", "CHECKED_IN", "COMPLETED", "CANCELLED", "NO_SHOW")
        val TICKET_STATUSES = setOf("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")
        val TICKET_PRIORITIES = setOf("LOW", "MEDIUM", "HIGH", "URGENT")
        val RELATION_TYPES = setOf("SPOUSE", "CHILD", "PARENT", "FAMILY", "CORPORATE", "MERGED")
        val OPEN_TICKET_STATUSES = listOf("OPEN", "IN_PROGRESS")
        val EARN_TYPES = setOf("EARN", "ADJUSTMENT_CREDIT")
        val BURN_TYPES = setOf("REDEEM", "REWARD_CLAIM", "ADJUSTMENT_DEBIT")
        val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val TICKET_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    // ---------- Search & context ----------

    @Transactional(readOnly = true)
    fun search(tenantId: Long, rawQuery: String): List<MemberSearchResult> {
        val query = rawQuery.trim()
        require(query.isNotEmpty()) { "Enter a member id, email, phone or name" }
        val exactById = query.toLongOrNull()?.let { memberRepository.findByIdAndTenantId(it, tenantId) }
        val matches = memberRepository.search(tenantId, query.take(120), PageRequest.of(0, 25))
        return (listOfNotNull(exactById) + matches).distinctBy { it.id }.map { member ->
            MemberSearchResult(
                id = member.id,
                externalUserId = member.externalUserId,
                email = member.email,
                firstName = member.firstName,
                lastName = member.lastName,
                phone = member.phone,
                dateOfBirth = member.dateOfBirth,
                tier = member.tier,
                status = member.status,
                accountBalance = accountRepository.findByTenantIdAndMemberIdAndAccountType(tenantId, member.id, "REDEMPTION")?.availablePoints ?: 0
            )
        }
    }

    @Transactional(readOnly = true)
    fun overview(tenantId: Long, memberId: Long, programId: Long?): Member360Response {
        val member = requireMember(tenantId, memberId)
        val program = resolveProgram(tenantId, programId)
        val now = Instant.now()
        val openTickets = ticketRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
            .filter { it.status in OPEN_TICKET_STATUSES }
        return Member360Response(
            profile = profileOf(member),
            programId = program?.id,
            programName = program?.name,
            currency = program?.currency,
            tier = tierSummary(member, program),
            balances = balances(tenantId, memberId, program, now),
            counts = MemberCountsResponse(
                transactions = transactionRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId).size.toLong(),
                activeVouchers = vouchers(tenantId, memberId).count { it.status == "ACTIVE" }.toLong(),
                eligibleOffers = if (program == null) 0 else offers(tenantId, memberId, program.id).count { it.eligible }.toLong(),
                bookings = memberBookingRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId).size.toLong(),
                openTickets = openTickets.size.toLong(),
                linkedMembers = memberLinkRepository.findAllForMember(tenantId, memberId).size.toLong() +
                    partnerMembershipRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId).size,
                activeCards = membershipCardRepository.findByTenantIdAndMemberIdOrderByIssuedAtDesc(tenantId, memberId).count { it.status == "ACTIVE" }.toLong()
            ),
            hotnotes = openTickets.filter { it.isHotnote }.map(ServiceTicketResponse::from)
        )
    }

    @Transactional(readOnly = true)
    fun hotnotes(tenantId: Long): List<HotnoteResponse> {
        val tickets = ticketRepository.findTop20ByTenantIdAndIsHotnoteTrueAndStatusInOrderByCreatedAtDesc(tenantId, OPEN_TICKET_STATUSES)
        val members = memberRepository.findAllById(tickets.map { it.memberId }.toSet())
            .filter { it.tenantId == tenantId }
            .associateBy { it.id }
        return tickets.map {
            HotnoteResponse(ServiceTicketResponse.from(it), members[it.memberId]?.externalUserId, members[it.memberId]?.email)
        }
    }

    // ---------- 1. Member details ----------

    @Transactional
    fun updateProfile(tenantId: Long, memberId: Long, request: MemberProfileUpdateRequest): MemberProfileResponse {
        val member = requireMember(tenantId, memberId)
        request.status?.let { require(it.uppercase() in MEMBER_STATUSES) { "Unsupported member status '$it'" } }
        request.enrollingSponsorId?.let { sponsorId ->
            require(sponsorRepository.findById(sponsorId).orElse(null)?.tenantId == tenantId) { "Enrolling sponsor does not belong to tenant" }
        }
        val email = request.email?.trim()?.ifBlank { null }
        if (email != null && !email.equals(member.email, ignoreCase = true)) {
            val existing = memberRepository.findFirstByTenantIdAndEmailIgnoreCase(tenantId, email)
            require(existing == null || existing.id == member.id) { "Email is already used by another member" }
        }
        val updated = memberRepository.save(
            member.copy(
                email = email ?: member.email,
                firstName = request.firstName?.trim() ?: member.firstName,
                lastName = request.lastName?.trim() ?: member.lastName,
                phone = request.phone?.trim() ?: member.phone,
                alternatePhone = request.alternatePhone?.trim() ?: member.alternatePhone,
                dateOfBirth = request.dateOfBirth ?: member.dateOfBirth,
                gender = request.gender?.trim() ?: member.gender,
                nationality = request.nationality?.trim() ?: member.nationality,
                preferredLanguage = request.preferredLanguage?.trim() ?: member.preferredLanguage,
                enrollingSponsorId = request.enrollingSponsorId ?: member.enrollingSponsorId,
                status = request.status?.uppercase() ?: member.status
            )
        )
        return profileOf(updated)
    }

    // ---------- 2. Linked members ----------

    @Transactional(readOnly = true)
    fun links(tenantId: Long, memberId: Long): List<MemberLinkResponse> {
        requireMember(tenantId, memberId)
        val links = memberLinkRepository.findAllForMember(tenantId, memberId)
        val otherIds = links.map { if (it.primaryMemberId == memberId) it.linkedMemberId else it.primaryMemberId }.toSet()
        val others = memberRepository.findAllById(otherIds).filter { it.tenantId == tenantId }.associateBy { it.id }
        val memberLinks = links.map { link ->
            val isPrimary = link.primaryMemberId == memberId
            val other = others[if (isPrimary) link.linkedMemberId else link.primaryMemberId]
            MemberLinkResponse(
                id = link.id,
                linkSource = "HOUSEHOLD",
                relationType = link.relationType,
                canSharePoints = link.canSharePoints,
                direction = if (isPrimary) "PRIMARY" else "LINKED_TO",
                memberId = other?.id,
                externalUserId = other?.externalUserId,
                email = other?.email,
                tier = other?.tier,
                sponsorId = null,
                sponsorName = null,
                status = other?.status ?: "UNKNOWN",
                createdAt = link.createdAt
            )
        }
        val partnerLinks = partnerMembershipRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
        val sponsorNames = sponsorNames(tenantId, partnerLinks.map { it.sponsorId })
        return memberLinks + partnerLinks.map {
            MemberLinkResponse(
                id = it.id,
                linkSource = "PARTNER",
                relationType = "PARTNER_MEMBERSHIP",
                canSharePoints = false,
                direction = "PARTNER",
                memberId = null,
                externalUserId = it.externalMembershipId,
                email = null,
                tier = null,
                sponsorId = it.sponsorId,
                sponsorName = sponsorNames[it.sponsorId],
                status = it.status,
                createdAt = it.createdAt
            )
        }
    }

    @Transactional
    fun createLink(tenantId: Long, memberId: Long, request: MemberLinkCreateRequest): List<MemberLinkResponse> {
        requireMember(tenantId, memberId)
        val relation = request.relationType.uppercase()
        require(relation in RELATION_TYPES) { "Unsupported relation type '$relation'" }
        val identifier = request.linkedMemberIdentifier.trim()
        val linked = identifier.toLongOrNull()?.let { memberRepository.findByIdAndTenantId(it, tenantId) }
            ?: memberRepository.findByTenantIdAndExternalUserId(tenantId, identifier)
            ?: memberRepository.findFirstByTenantIdAndEmailIgnoreCase(tenantId, identifier)
            ?: throw NoSuchElementException("No member found for '$identifier'")
        require(linked.id != memberId) { "A member cannot be linked to themselves" }
        require(
            !memberLinkRepository.existsByTenantIdAndPrimaryMemberIdAndLinkedMemberId(tenantId, memberId, linked.id) &&
                !memberLinkRepository.existsByTenantIdAndPrimaryMemberIdAndLinkedMemberId(tenantId, linked.id, memberId)
        ) { "Members are already linked" }
        memberLinkRepository.save(
            MemberLinkEntity(
                tenantId = tenantId,
                primaryMemberId = memberId,
                linkedMemberId = linked.id,
                relationType = relation,
                canSharePoints = request.canSharePoints
            )
        )
        return links(tenantId, memberId)
    }

    @Transactional
    fun deleteLink(tenantId: Long, memberId: Long, linkId: Long) {
        val link = memberLinkRepository.findByTenantIdAndId(tenantId, linkId)
            ?.takeIf { it.primaryMemberId == memberId || it.linkedMemberId == memberId }
            ?: throw NoSuchElementException("Link not found")
        memberLinkRepository.delete(link)
    }

    // ---------- 3. Membership cards ----------

    @Transactional(readOnly = true)
    fun cards(tenantId: Long, memberId: Long): List<MembershipCardResponse> {
        requireMember(tenantId, memberId)
        return membershipCardRepository.findByTenantIdAndMemberIdOrderByIssuedAtDesc(tenantId, memberId).map(MembershipCardResponse::from)
    }

    @Transactional
    fun issueCard(tenantId: Long, memberId: Long, request: MembershipCardIssueRequest): MembershipCardResponse {
        requireMember(tenantId, memberId)
        val cardType = request.cardType.uppercase()
        require(cardType in setOf("DIGITAL", "PHYSICAL", "APPLE_WALLET", "GOOGLE_WALLET")) { "Unsupported card type '$cardType'" }
        if (request.replaceExisting) {
            membershipCardRepository.findByTenantIdAndMemberIdOrderByIssuedAtDesc(tenantId, memberId)
                .filter { it.cardType == cardType && it.status == "ACTIVE" }
                .forEach { membershipCardRepository.save(it.copy(status = "REPLACED")) }
        }
        var cardNumber: String
        do {
            cardNumber = "BNV" + (1..13).joinToString("") { random.nextInt(10).toString() }
        } while (membershipCardRepository.existsByTenantIdAndCardNumber(tenantId, cardNumber))
        val now = Instant.now()
        val saved = membershipCardRepository.save(
            MembershipCardEntity(
                tenantId = tenantId,
                memberId = memberId,
                cardNumber = cardNumber,
                cardType = cardType,
                barcodePayload = cardNumber,
                issuedAt = now,
                expiresAt = request.validityMonths?.takeIf { it > 0 }?.let {
                    now.atZone(ZoneOffset.UTC).plusMonths(it.toLong()).toInstant()
                }
            )
        )
        return MembershipCardResponse.from(saved)
    }

    @Transactional
    fun updateCardStatus(tenantId: Long, memberId: Long, cardId: Long, status: String): MembershipCardResponse {
        val normalized = status.uppercase()
        require(normalized in CARD_STATUSES) { "Unsupported card status '$status'" }
        val card = membershipCardRepository.findByTenantIdAndMemberIdAndId(tenantId, memberId, cardId)
            ?: throw NoSuchElementException("Card not found")
        return MembershipCardResponse.from(membershipCardRepository.save(card.copy(status = normalized)))
    }

    // ---------- 4. Balance & FIFO lots ----------

    @Transactional(readOnly = true)
    fun balanceDetail(tenantId: Long, memberId: Long, programId: Long?): MemberBalanceDetailResponse {
        requireMember(tenantId, memberId)
        val now = Instant.now()
        val program = resolveProgram(tenantId, programId)
        val lots = walletHistoryRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
            .map { PointLotResponse.from(it, now) }
        return MemberBalanceDetailResponse(balances(tenantId, memberId, program, now), lots)
    }

    // ---------- 5. Transactions ----------

    @Transactional(readOnly = true)
    fun transactions(tenantId: Long, memberId: Long): List<MemberTransactionResponse> {
        requireMember(tenantId, memberId)
        val transactions = transactionRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
        val names = sponsorNames(tenantId, transactions.mapNotNull { it.sponsorId })
        return transactions.map { MemberTransactionResponse.from(it, it.sponsorId?.let(names::get)) }
    }

    // ---------- 6. Vouchers ----------

    @Transactional(readOnly = true)
    fun vouchers(tenantId: Long, memberId: Long): List<MemberVoucherResponse> {
        val now = Instant.now()
        val vouchers = offerVoucherRepository.findByTenantIdAndIssuedToMemberIdOrderByIssuedAtDesc(tenantId, memberId)
        val offers = offerRepository.findAllById(vouchers.map { it.offerId }.toSet())
            .filter { it.tenantId == tenantId }
            .associateBy { it.id }
        return vouchers.map { voucher ->
            val offer = offers[voucher.offerId]
            MemberVoucherResponse(
                id = voucher.id,
                voucherCode = voucher.voucherCode,
                offerId = voucher.offerId,
                offerName = offer?.name,
                offerCategory = offer?.category,
                issuedAt = voucher.issuedAt,
                expiresAt = voucher.expiresAt,
                referenceId = voucher.referenceId,
                status = if (voucher.expiresAt != null && voucher.expiresAt <= now) "EXPIRED" else "ACTIVE"
            )
        }
    }

    // ---------- 7. Offers (public + MTO) ----------

    @Transactional(readOnly = true)
    fun offers(tenantId: Long, memberId: Long, programId: Long?): List<MemberOfferResponse> {
        val member = requireMember(tenantId, memberId)
        val program = resolveProgram(tenantId, programId) ?: return emptyList()
        val now = Instant.now()
        val tierRank = tierRepository.findByTenantIdAndProgramIdOrderByRank(tenantId, program.id)
            .firstOrNull { it.name == member.tier }?.rank ?: 0
        return offerRepository
            .findByTenantIdAndProgramIdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tenantId, program.id, now, now)
            .filter { it.memberVisibility }
            .mapNotNull { offer ->
                val targeted = offer.isMto && offerTargetMemberRepository.existsByOfferIdAndMemberId(offer.id, memberId)
                if (offer.isMto && !targeted) return@mapNotNull null
                val uses = offerApplicationRepository.countByTenantIdAndMemberIdAndOfferId(tenantId, memberId, offer.id)
                val reason = ineligibleReason(offer, tierRank, uses)
                MemberOfferResponse(
                    id = offer.id,
                    offerCode = offer.offerCode,
                    name = offer.name,
                    description = offer.description,
                    category = offer.category,
                    offerType = offer.offerType,
                    status = offer.status,
                    isMto = offer.isMto,
                    isTargeted = targeted,
                    multiplier = offer.multiplier,
                    bonusPoints = offer.bonusPoints,
                    pointsRequired = offer.pointsRequired,
                    minTierRank = offer.minTierRank,
                    maxUsesPerMember = offer.maxUsesPerMember,
                    usesByMember = uses,
                    startDate = offer.startDate,
                    endDate = offer.endDate,
                    eligible = reason == null,
                    ineligibleReason = reason
                )
            }
    }

    // ---------- 8. Bookings ----------

    @Transactional(readOnly = true)
    fun bookings(tenantId: Long, memberId: Long): List<MemberBookingResponse> {
        requireMember(tenantId, memberId)
        val bookings = memberBookingRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
        val names = sponsorNames(tenantId, bookings.mapNotNull { it.sponsorId })
        return bookings.map { MemberBookingResponse.from(it, it.sponsorId?.let(names::get)) }
    }

    @Transactional
    fun createBooking(tenantId: Long, memberId: Long, programId: Long?, request: MemberBookingCreateRequest): MemberBookingResponse {
        requireMember(tenantId, memberId)
        val reference = request.bookingReference.trim()
        require(!memberBookingRepository.existsByTenantIdAndBookingReference(tenantId, reference)) { "Booking reference already exists" }
        val status = request.status.uppercase()
        require(status in BOOKING_STATUSES) { "Unsupported booking status '$status'" }
        require(request.totalAmount >= BigDecimal.ZERO) { "Total amount cannot be negative" }
        if (request.checkInDate != null && request.checkOutDate != null) {
            require(!request.checkOutDate.isBefore(request.checkInDate)) { "Check-out must be on or after check-in" }
        }
        val sponsorName = request.sponsorId?.let { sponsorId ->
            val sponsor = sponsorRepository.findById(sponsorId).orElse(null)
            require(sponsor != null && sponsor.tenantId == tenantId) { "Sponsor does not belong to tenant" }
            sponsor.name
        }
        val saved = memberBookingRepository.save(
            MemberBookingEntity(
                tenantId = tenantId,
                memberId = memberId,
                bookingReference = reference,
                sponsorId = request.sponsorId,
                locationId = request.locationId,
                bookingType = request.bookingType.uppercase(),
                status = status,
                checkInDate = request.checkInDate,
                checkOutDate = request.checkOutDate,
                roomType = request.roomType,
                roomNumber = request.roomNumber,
                totalAmount = request.totalAmount,
                currency = request.currency?.ifBlank { null } ?: resolveProgram(tenantId, programId)?.currency ?: "INR",
                notes = request.notes
            )
        )
        return MemberBookingResponse.from(saved, sponsorName)
    }

    @Transactional
    fun updateBookingStatus(tenantId: Long, memberId: Long, bookingId: Long, status: String): MemberBookingResponse {
        val normalized = status.uppercase()
        require(normalized in BOOKING_STATUSES) { "Unsupported booking status '$status'" }
        val booking = memberBookingRepository.findByTenantIdAndMemberIdAndId(tenantId, memberId, bookingId)
            ?: throw NoSuchElementException("Booking not found")
        val saved = memberBookingRepository.save(booking.copy(status = normalized))
        return MemberBookingResponse.from(saved, saved.sponsorId?.let { sponsorNames(tenantId, listOf(it))[it] })
    }

    // ---------- 9. Services, notes & CS actions ----------

    @Transactional(readOnly = true)
    fun tickets(tenantId: Long, memberId: Long): List<ServiceTicketResponse> {
        requireMember(tenantId, memberId)
        return ticketRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId).map(ServiceTicketResponse::from)
    }

    @Transactional
    fun createTicket(tenantId: Long, memberId: Long, authUserId: Long?, request: ServiceTicketCreateRequest): ServiceTicketResponse {
        requireMember(tenantId, memberId)
        val priority = request.priority.uppercase()
        require(priority in TICKET_PRIORITIES) { "Unsupported priority '$priority'" }
        return ServiceTicketResponse.from(
            saveTicket(
                tenantId = tenantId,
                memberId = memberId,
                authUserId = authUserId,
                category = request.category.uppercase(),
                priority = priority,
                status = "OPEN",
                subject = request.subject.trim(),
                description = request.description,
                isHotnote = request.isHotnote
            )
        )
    }

    @Transactional
    fun updateTicket(tenantId: Long, memberId: Long, ticketId: Long, request: ServiceTicketUpdateRequest): ServiceTicketResponse {
        val ticket = ticketRepository.findByTenantIdAndMemberIdAndId(tenantId, memberId, ticketId)
            ?: throw NoSuchElementException("Ticket not found")
        val status = request.status?.uppercase()?.also { require(it in TICKET_STATUSES) { "Unsupported status '$it'" } } ?: ticket.status
        val priority = request.priority?.uppercase()?.also { require(it in TICKET_PRIORITIES) { "Unsupported priority '$it'" } } ?: ticket.priority
        val resolved = status in setOf("RESOLVED", "CLOSED")
        return ServiceTicketResponse.from(
            ticketRepository.save(
                ticket.copy(
                    status = status,
                    priority = priority,
                    resolutionNotes = request.resolutionNotes ?: ticket.resolutionNotes,
                    isHotnote = request.isHotnote ?: ticket.isHotnote,
                    resolvedAt = if (resolved) ticket.resolvedAt ?: Instant.now() else null
                )
            )
        )
    }

    @Transactional
    fun adjustPoints(tenantId: Long, memberId: Long, programId: Long?, authUserId: Long?, request: PointAdjustmentRequest): PointAdjustmentResponse {
        val member = memberRepository.findLockedByIdAndTenantId(memberId, tenantId) ?: throw NoSuchElementException("Member $memberId not found")
        val direction = request.direction.uppercase()
        require(direction == "CREDIT" || direction == "DEBIT") { "Direction must be CREDIT or DEBIT" }
        require(request.points in 1L..10_000_000L) { "Points must be between 1 and 10,000,000" }
        val program = resolveProgram(tenantId, programId)
        val now = Instant.now()
        val account = accountRepository.findLockedByTenantIdAndMemberIdAndAccountType(tenantId, member.id, "REDEMPTION")
            ?: accountRepository.save(AccountEntity(tenantId = tenantId, memberId = member.id, accountType = "REDEMPTION"))
        val reason = request.reason.trim()

        val updatedAccount = if (direction == "CREDIT") {
            accountRepository.save(
                account.copy(
                    availablePoints = account.availablePoints + request.points,
                    lifetimeEarnedPoints = account.lifetimeEarnedPoints + request.points,
                    updatedAt = now
                )
            )
        } else {
            require(account.availablePoints >= request.points) { "Insufficient balance. Available: ${account.availablePoints}" }
            val lots = walletHistoryRepository.findLockedUnexpiredSpendableCredits(tenantId, member.id, now)
            require(lots.sumOf { it.remainingPoints } >= request.points) { "Insufficient unexpired point lots for this debit" }
            var remaining = request.points
            lots.forEach { lot ->
                if (remaining > 0) {
                    val consumed = minOf(lot.remainingPoints, remaining)
                    walletHistoryRepository.save(lot.copy(remainingPoints = lot.remainingPoints - consumed))
                    remaining -= consumed
                }
            }
            accountRepository.save(account.copy(availablePoints = account.availablePoints - request.points, updatedAt = now))
        }

        val transactionType = "ADJUSTMENT_$direction"
        val transaction = transactionRepository.save(
            TransactionEntity(
                tenantId = tenantId,
                programId = program?.id,
                memberId = member.id,
                accountId = updatedAccount.id,
                eventType = request.category.uppercase(),
                transactionType = transactionType,
                points = request.points,
                status = "APPROVED",
                referenceId = "ADJ-${UUID.randomUUID()}",
                channel = "CS_CONSOLE",
                createdAt = now
            )
        )
        walletHistoryRepository.save(
            WalletHistoryEntity(
                tenantId = tenantId,
                programId = program?.id,
                memberId = member.id,
                accountId = updatedAccount.id,
                accountType = "REDEMPTION",
                entryType = direction,
                points = request.points,
                description = "CS $direction adjustment (${request.category.uppercase()}): $reason",
                expiresAt = if (direction == "CREDIT") program?.let { pointExpiryPolicyService.expiresAt(it, now) } else null,
                remainingPoints = if (direction == "CREDIT") request.points else 0,
                createdAt = now
            )
        )
        val ticket = saveTicket(
            tenantId = tenantId,
            memberId = member.id,
            authUserId = authUserId,
            category = "POINT_ADJUSTMENT",
            priority = "MEDIUM",
            status = "RESOLVED",
            subject = "${if (direction == "CREDIT") "+" else "-"}${request.points} pts ${request.category.uppercase()}",
            description = reason,
            pointsAdjusted = if (direction == "CREDIT") request.points else -request.points,
            resolutionNotes = "Transaction #${transaction.id}"
        )
        return PointAdjustmentResponse(transaction.id, direction, request.points, updatedAccount.availablePoints, ServiceTicketResponse.from(ticket))
    }

    @Transactional
    fun overrideTier(tenantId: Long, memberId: Long, programId: Long?, authUserId: Long?, request: TierOverrideRequest): Member360Response {
        val member = memberRepository.findLockedByIdAndTenantId(memberId, tenantId) ?: throw NoSuchElementException("Member $memberId not found")
        val program = resolveProgram(tenantId, programId) ?: throw IllegalArgumentException("No program configured for tenant")
        val target = tierRepository.findByTenantIdAndProgramIdOrderByRank(tenantId, program.id)
            .firstOrNull { it.name.equals(request.targetTier.trim(), ignoreCase = true) }
            ?: throw IllegalArgumentException("Tier '${request.targetTier}' is not defined for this program")
        require(target.name != member.tier) { "Member is already in tier ${target.name}" }
        memberRepository.save(member.copy(tier = target.name))
        saveTicket(
            tenantId = tenantId,
            memberId = member.id,
            authUserId = authUserId,
            category = "TIER_OVERRIDE",
            priority = "MEDIUM",
            status = "RESOLVED",
            subject = "Tier changed ${member.tier} → ${target.name}",
            description = request.reason.trim()
        )
        return overview(tenantId, memberId, program.id)
    }

    // ---------- 10. KPIs ----------

    @Transactional(readOnly = true)
    fun kpis(tenantId: Long, memberId: Long, programId: Long?): MemberKpiResponse {
        val member = requireMember(tenantId, memberId)
        val program = resolveProgram(tenantId, programId)
        val now = Instant.now()
        val transactions = transactionRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
            .filter { it.status == "APPROVED" }
        val earnTransactions = transactions.filter { it.transactionType == "EARN" }
        val spend = earnTransactions.sumOf { it.amount }
        val earned = transactions.filter { it.transactionType in EARN_TYPES }.sumOf { it.points }
        val redeemed = transactions.filter { it.transactionType in BURN_TYPES }.sumOf { it.points }
        val expired = transactions.filter { it.transactionType == "EXPIRE" }.sumOf { it.points }
        val lastActivity = transactions.maxOfOrNull { it.createdAt }
        val bookings = memberBookingRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
        val completed = bookings.filter { it.status == "COMPLETED" }
        val currentMonth = YearMonth.now(ZoneOffset.UTC)
        val monthly = (11 downTo 0).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            val inMonth = transactions.filter { YearMonth.from(it.createdAt.atZone(ZoneOffset.UTC)) == month }
            MonthlyActivity(
                month = month.format(MONTH_FORMAT),
                spend = inMonth.filter { it.transactionType == "EARN" }.sumOf { it.amount },
                pointsEarned = inMonth.filter { it.transactionType in EARN_TYPES }.sumOf { it.points },
                pointsRedeemed = inMonth.filter { it.transactionType in BURN_TYPES }.sumOf { it.points },
                transactions = inMonth.size.toLong()
            )
        }
        return MemberKpiResponse(
            lifetimeSpend = spend,
            earnTransactions = earnTransactions.size.toLong(),
            averageOrderValue = if (earnTransactions.isEmpty()) 0 else spend / earnTransactions.size,
            lifetimePointsEarned = earned,
            lifetimePointsRedeemed = redeemed,
            lifetimePointsExpired = expired,
            redemptionRatePercent = if (earned == 0L) 0 else ((redeemed * 100) / earned).toInt().coerceAtMost(100),
            visitsLast90Days = earnTransactions.count { it.createdAt.isAfter(now.minus(Duration.ofDays(90))) }.toLong(),
            daysSinceLastActivity = lastActivity?.let { ChronoUnit.DAYS.between(it, now) },
            firstActivityAt = transactions.minOfOrNull { it.createdAt },
            lastActivityAt = lastActivity,
            totalBookings = bookings.size.toLong(),
            completedStays = completed.size.toLong(),
            totalNights = completed.sumOf { b ->
                if (b.checkInDate != null && b.checkOutDate != null) ChronoUnit.DAYS.between(b.checkInDate, b.checkOutDate) else 0L
            },
            bookingRevenue = completed.fold(BigDecimal.ZERO) { acc, b -> acc + b.totalAmount },
            offersRedeemed = offerApplicationRepository.countByTenantIdAndMemberId(tenantId, memberId),
            openTickets = ticketRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
                .count { it.status in OPEN_TICKET_STATUSES }.toLong(),
            tier = tierSummary(member, program),
            monthly = monthly
        )
    }

    // ---------- helpers ----------

    private fun requireMember(tenantId: Long, memberId: Long): MemberEntity =
        memberRepository.findByIdAndTenantId(memberId, tenantId) ?: throw NoSuchElementException("Member $memberId not found")

    private fun resolveProgram(tenantId: Long, programId: Long?): ProgramEntity? {
        programId?.let { id ->
            programRepository.findById(id).orElse(null)?.takeIf { it.tenantId == tenantId }?.let { return it }
        }
        return programRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).firstOrNull()
    }

    private fun profileOf(member: MemberEntity): MemberProfileResponse {
        val sponsorName = member.enrollingSponsorId?.let { sponsorNames(member.tenantId, listOf(it))[it] }
        return MemberProfileResponse.from(member, sponsorName)
    }

    private fun sponsorNames(tenantId: Long, ids: Collection<Long>): Map<Long, String> =
        if (ids.isEmpty()) emptyMap()
        else sponsorRepository.findAllById(ids.toSet()).filter { it.tenantId == tenantId }.associate { it.id to it.name }

    private fun tierSummary(member: MemberEntity, program: ProgramEntity?): TierSummaryResponse {
        val tiers = program?.let { tierRepository.findByTenantIdAndProgramIdOrderByRank(member.tenantId, it.id) }.orEmpty()
        val recognition = accountRepository.findByTenantIdAndMemberIdAndAccountType(member.tenantId, member.id, "RECOGNITION")
            ?.lifetimeEarnedPoints ?: 0
        val current = tiers.firstOrNull { it.name == member.tier }
        val next = tiers.filter { it.rank > (current?.rank ?: Int.MIN_VALUE) }.minByOrNull { it.rank }
        val floor = current?.thresholdPoints ?: 0
        val progress = when {
            next == null -> 100
            next.thresholdPoints <= floor -> 100
            else -> (((recognition - floor) * 100) / (next.thresholdPoints - floor)).toInt().coerceIn(0, 100)
        }
        return TierSummaryResponse(
            tierName = member.tier,
            tierRank = current?.rank ?: 0,
            multiplier = current?.multiplier ?: BigDecimal.ONE,
            thresholdPoints = floor,
            nextTierName = next?.name,
            nextTierThreshold = next?.thresholdPoints,
            pointsToNextTier = next?.let { maxOf(0, it.thresholdPoints - recognition) },
            progressPercent = progress
        )
    }

    private fun balances(tenantId: Long, memberId: Long, program: ProgramEntity?, now: Instant): MemberBalancesResponse {
        val redemption = accountRepository.findByTenantIdAndMemberIdAndAccountType(tenantId, memberId, "REDEMPTION")
        val recognition = accountRepository.findByTenantIdAndMemberIdAndAccountType(tenantId, memberId, "RECOGNITION")
        val warningDays = program?.expiryWarningDays ?: 30
        val warningCutoff = now.plus(Duration.ofDays(warningDays.toLong()))
        val liveLots = walletHistoryRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
            .filter { it.accountType == "REDEMPTION" && it.entryType == "CREDIT" && !it.isExpired && it.remainingPoints > 0 }
            .filter { it.expiresAt == null || it.expiresAt.isAfter(now) }
        val expiring = liveLots.filter { it.expiresAt != null && !it.expiresAt.isAfter(warningCutoff) }
        return MemberBalancesResponse(
            spendablePoints = redemption?.availablePoints ?: 0,
            pendingPoints = redemption?.pendingPoints ?: 0,
            redeemedPoints = redemption?.redeemedPoints ?: 0,
            lifetimeEarnedPoints = redemption?.lifetimeEarnedPoints ?: 0,
            recognitionPoints = recognition?.availablePoints ?: 0,
            lifetimeRecognitionPoints = recognition?.lifetimeEarnedPoints ?: 0,
            pointsExpiringSoon = expiring.sumOf { it.remainingPoints },
            expiryWarningDays = warningDays,
            nextExpiryDate = liveLots.mapNotNull { it.expiresAt }.minOrNull()
        )
    }

    private fun ineligibleReason(offer: OfferEntity, tierRank: Int, uses: Long): String? = when {
        offer.status != "LAUNCHED" -> "Offer is ${offer.status}"
        tierRank < offer.minTierRank -> "Requires tier rank ${offer.minTierRank}"
        offer.maxUsesPerMember != null && uses >= offer.maxUsesPerMember -> "Member usage limit reached"
        offer.maxTotalClaims != null && offer.totalClaimsCount >= offer.maxTotalClaims -> "Offer fully claimed"
        else -> null
    }

    private fun saveTicket(
        tenantId: Long,
        memberId: Long,
        authUserId: Long?,
        category: String,
        priority: String,
        status: String,
        subject: String,
        description: String?,
        isHotnote: Boolean = false,
        pointsAdjusted: Long = 0,
        resolutionNotes: String? = null
    ): MemberServiceTicketEntity {
        val now = Instant.now()
        val suffix = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[random.nextInt(32)] }.joinToString("")
        return ticketRepository.save(
            MemberServiceTicketEntity(
                tenantId = tenantId,
                memberId = memberId,
                ticketReference = "SR-${now.atZone(ZoneOffset.UTC).format(TICKET_DATE_FORMAT)}-$suffix",
                category = category,
                priority = priority,
                status = status,
                subject = subject.take(255),
                description = description?.take(4000),
                isHotnote = isHotnote,
                createdByUserId = authUserId,
                pointsAdjusted = pointsAdjusted,
                resolutionNotes = resolutionNotes,
                createdAt = now,
                resolvedAt = if (status == "RESOLVED" || status == "CLOSED") now else null
            )
        )
    }
}
