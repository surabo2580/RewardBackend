package com.reward.platform.api.controller

import com.reward.platform.api.dto.HotnoteResponse
import com.reward.platform.api.dto.Member360Response
import com.reward.platform.api.dto.MemberBalanceDetailResponse
import com.reward.platform.api.dto.MemberBookingCreateRequest
import com.reward.platform.api.dto.MemberBookingResponse
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
import com.reward.platform.api.dto.PointAdjustmentRequest
import com.reward.platform.api.dto.PointAdjustmentResponse
import com.reward.platform.api.dto.ServiceTicketCreateRequest
import com.reward.platform.api.dto.ServiceTicketResponse
import com.reward.platform.api.dto.ServiceTicketUpdateRequest
import com.reward.platform.api.dto.StatusUpdateRequest
import com.reward.platform.api.dto.TierOverrideRequest
import com.reward.platform.api.service.Member360Service
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Member 360° workspace: search a member, then every member-scoped module (GRAVTY-style contextual sidebar). */
@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/members")
class Member360Controller(
    private val member360Service: Member360Service
) {

    @GetMapping("/search")
    fun search(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestParam("q") query: String
    ): ResponseEntity<List<MemberSearchResult>> = ResponseEntity.ok(member360Service.search(tenantId, query))

    @GetMapping("/hotnotes")
    fun hotnotes(@RequestAttribute("tenantId") tenantId: Long): ResponseEntity<List<HotnoteResponse>> =
        ResponseEntity.ok(member360Service.hotnotes(tenantId))

    @GetMapping("/{memberId:\\d+}/360")
    fun overview(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "programId", required = false) programId: Long?,
        @PathVariable memberId: Long
    ): ResponseEntity<Member360Response> = ResponseEntity.ok(member360Service.overview(tenantId, memberId, programId))

    @PutMapping("/{memberId:\\d+}/profile")
    fun updateProfile(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @RequestBody request: MemberProfileUpdateRequest
    ): ResponseEntity<MemberProfileResponse> = ResponseEntity.ok(member360Service.updateProfile(tenantId, memberId, request))

    @GetMapping("/{memberId:\\d+}/links")
    fun links(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<List<MemberLinkResponse>> = ResponseEntity.ok(member360Service.links(tenantId, memberId))

    @PostMapping("/{memberId:\\d+}/links")
    fun createLink(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: MemberLinkCreateRequest
    ): ResponseEntity<List<MemberLinkResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(member360Service.createLink(tenantId, memberId, request))

    @DeleteMapping("/{memberId:\\d+}/links/{linkId}")
    fun deleteLink(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @PathVariable linkId: Long
    ): ResponseEntity<Void> {
        member360Service.deleteLink(tenantId, memberId, linkId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{memberId:\\d+}/cards")
    fun cards(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<List<MembershipCardResponse>> = ResponseEntity.ok(member360Service.cards(tenantId, memberId))

    @PostMapping("/{memberId:\\d+}/cards")
    fun issueCard(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @RequestBody request: MembershipCardIssueRequest
    ): ResponseEntity<MembershipCardResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(member360Service.issueCard(tenantId, memberId, request))

    @PatchMapping("/{memberId:\\d+}/cards/{cardId}/status")
    fun updateCardStatus(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @PathVariable cardId: Long,
        @Valid @RequestBody request: StatusUpdateRequest
    ): ResponseEntity<MembershipCardResponse> =
        ResponseEntity.ok(member360Service.updateCardStatus(tenantId, memberId, cardId, request.status))

    @GetMapping("/{memberId:\\d+}/balance")
    fun balance(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "programId", required = false) programId: Long?,
        @PathVariable memberId: Long
    ): ResponseEntity<MemberBalanceDetailResponse> = ResponseEntity.ok(member360Service.balanceDetail(tenantId, memberId, programId))

    @GetMapping("/{memberId:\\d+}/transactions")
    fun transactions(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<List<MemberTransactionResponse>> = ResponseEntity.ok(member360Service.transactions(tenantId, memberId))

    @GetMapping("/{memberId:\\d+}/vouchers")
    fun vouchers(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<List<MemberVoucherResponse>> = ResponseEntity.ok(member360Service.vouchers(tenantId, memberId))

    @GetMapping("/{memberId:\\d+}/offers")
    fun offers(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "programId", required = false) programId: Long?,
        @PathVariable memberId: Long
    ): ResponseEntity<List<MemberOfferResponse>> = ResponseEntity.ok(member360Service.offers(tenantId, memberId, programId))

    @GetMapping("/{memberId:\\d+}/bookings")
    fun bookings(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<List<MemberBookingResponse>> = ResponseEntity.ok(member360Service.bookings(tenantId, memberId))

    @PostMapping("/{memberId:\\d+}/bookings")
    fun createBooking(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "programId", required = false) programId: Long?,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: MemberBookingCreateRequest
    ): ResponseEntity<MemberBookingResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(member360Service.createBooking(tenantId, memberId, programId, request))

    @PatchMapping("/{memberId:\\d+}/bookings/{bookingId}/status")
    fun updateBookingStatus(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @PathVariable bookingId: Long,
        @Valid @RequestBody request: StatusUpdateRequest
    ): ResponseEntity<MemberBookingResponse> =
        ResponseEntity.ok(member360Service.updateBookingStatus(tenantId, memberId, bookingId, request.status))

    @GetMapping("/{memberId:\\d+}/services")
    fun tickets(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<List<ServiceTicketResponse>> = ResponseEntity.ok(member360Service.tickets(tenantId, memberId))

    @PostMapping("/{memberId:\\d+}/services")
    fun createTicket(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "authUserId", required = false) authUserId: Long?,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: ServiceTicketCreateRequest
    ): ResponseEntity<ServiceTicketResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(member360Service.createTicket(tenantId, memberId, authUserId, request))

    @PatchMapping("/{memberId:\\d+}/services/{ticketId}")
    fun updateTicket(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable memberId: Long,
        @PathVariable ticketId: Long,
        @RequestBody request: ServiceTicketUpdateRequest
    ): ResponseEntity<ServiceTicketResponse> = ResponseEntity.ok(member360Service.updateTicket(tenantId, memberId, ticketId, request))

    @PostMapping("/{memberId:\\d+}/adjustments")
    fun adjustPoints(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "programId", required = false) programId: Long?,
        @RequestAttribute(name = "authUserId", required = false) authUserId: Long?,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: PointAdjustmentRequest
    ): ResponseEntity<PointAdjustmentResponse> =
        ResponseEntity.ok(member360Service.adjustPoints(tenantId, memberId, programId, authUserId, request))

    @PostMapping("/{memberId:\\d+}/tier-override")
    fun overrideTier(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "programId", required = false) programId: Long?,
        @RequestAttribute(name = "authUserId", required = false) authUserId: Long?,
        @PathVariable memberId: Long,
        @Valid @RequestBody request: TierOverrideRequest
    ): ResponseEntity<Member360Response> =
        ResponseEntity.ok(member360Service.overrideTier(tenantId, memberId, programId, authUserId, request))

    @GetMapping("/{memberId:\\d+}/kpis")
    fun kpis(
        @RequestAttribute("tenantId") tenantId: Long,
        @RequestAttribute(name = "programId", required = false) programId: Long?,
        @PathVariable memberId: Long
    ): ResponseEntity<MemberKpiResponse> = ResponseEntity.ok(member360Service.kpis(tenantId, memberId, programId))
}
