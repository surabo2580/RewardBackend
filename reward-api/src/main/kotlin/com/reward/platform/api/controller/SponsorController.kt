package com.reward.platform.api.controller

import com.reward.platform.api.dto.SponsorCreateRequest
import com.reward.platform.api.dto.SponsorLocationCreateRequest
import com.reward.platform.api.dto.SponsorLocationResponse
import com.reward.platform.api.dto.SponsorResponse
import com.reward.platform.api.entity.SponsorEntity
import com.reward.platform.api.entity.SponsorLocationEntity
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SponsorRepository
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/sponsors")
class SponsorController(
    private val sponsorRepository: SponsorRepository,
    private val locationRepository: SponsorLocationRepository,
    private val programRepository: ProgramRepository
) {
    @PostMapping
    fun createSponsor(
        @RequestAttribute("tenantId") tenantId: Long,
        @Valid @RequestBody request: SponsorCreateRequest
    ): ResponseEntity<SponsorResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        require(programRepository.findById(request.programId).orElse(null)?.tenantId == request.tenantId) {
            "Program does not belong to tenant"
        }
        request.parentSponsorId?.let { parentId ->
            val parent = sponsorRepository.findById(parentId).orElse(null)
            require(parent != null && parent.tenantId == request.tenantId && parent.programId == request.programId) {
                "Parent sponsor does not belong to tenant and program"
            }
        }
        return ResponseEntity.ok(SponsorResponse.from(sponsorRepository.save(SponsorEntity(
            tenantId = request.tenantId, programId = request.programId,
            parentSponsorId = request.parentSponsorId, name = request.name,
            sponsorCode = request.sponsorCode, status = request.status
        ))))
    }

    @GetMapping
    fun listSponsors(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @RequestParam programId: Long
    ): ResponseEntity<List<SponsorResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        return ResponseEntity.ok(sponsorRepository.findByTenantIdAndProgramIdOrderByName(tenantId, programId).map(SponsorResponse::from))
    }

    @PostMapping("/{sponsorId}/locations")
    fun createLocation(
        @RequestAttribute("tenantId") tenantId: Long,
        @PathVariable sponsorId: Long,
        @Valid @RequestBody request: SponsorLocationCreateRequest
    ): ResponseEntity<SponsorLocationResponse> {
        require(request.tenantId == tenantId) { "Tenant does not match API key" }
        val sponsor = sponsorRepository.findById(sponsorId).orElse(null)
        require(sponsor != null && sponsor.tenantId == request.tenantId) { "Sponsor does not belong to tenant" }
        return ResponseEntity.ok(SponsorLocationResponse.from(locationRepository.save(SponsorLocationEntity(
            tenantId = request.tenantId, sponsorId = sponsorId,
            locationName = request.locationName, locationCode = request.locationCode,
            address = request.address, latitude = request.latitude, longitude = request.longitude,
            status = request.status
        ))))
    }

    @GetMapping("/{sponsorId}/locations")
    fun listLocations(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @PathVariable sponsorId: Long,
        @RequestParam tenantId: Long
    ): ResponseEntity<List<SponsorLocationResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        return ResponseEntity.ok(locationRepository.findByTenantIdAndSponsorIdOrderByLocationName(tenantId, sponsorId).map(SponsorLocationResponse::from))
    }
}