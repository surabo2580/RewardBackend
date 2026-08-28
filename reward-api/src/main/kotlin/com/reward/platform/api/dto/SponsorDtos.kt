package com.reward.platform.api.dto

import com.reward.platform.api.entity.SponsorEntity
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class SponsorCreateRequest(
    val tenantId: Long = 0,
    val programId: Long = 0,
    val parentSponsorId: Long? = null,
    @field:NotBlank val name: String = "",
    @field:NotBlank val sponsorCode: String = "",
    val status: String = "ACTIVE"
)

data class SponsorResponse(
    val id: Long,
    val tenantId: Long,
    val programId: Long,
    val parentSponsorId: Long?,
    val name: String,
    val sponsorCode: String,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: SponsorEntity) = SponsorResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            programId = entity.programId,
            parentSponsorId = entity.parentSponsorId,
            name = entity.name,
            sponsorCode = entity.sponsorCode,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}

data class SponsorLocationCreateRequest(
    val tenantId: Long = 0,
    val sponsorId: Long = 0,
    @field:NotBlank val locationName: String = "",
    @field:NotBlank val locationCode: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "ACTIVE"
)

data class SponsorLocationResponse(
    val id: Long,
    val tenantId: Long,
    val sponsorId: Long,
    val locationName: String,
    val locationCode: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: com.reward.platform.api.entity.SponsorLocationEntity) = SponsorLocationResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            sponsorId = entity.sponsorId,
            locationName = entity.locationName,
            locationCode = entity.locationCode,
            address = entity.address,
            latitude = entity.latitude,
            longitude = entity.longitude,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}