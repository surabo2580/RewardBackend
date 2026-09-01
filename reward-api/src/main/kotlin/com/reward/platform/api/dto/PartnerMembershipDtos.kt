package com.reward.platform.api.dto

import com.reward.platform.api.entity.PartnerMembershipEntity
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class PartnerMembershipCreateRequest(
    val tenantId: Long = 0,
    val sponsorId: Long = 0,
    @field:NotBlank(message = "External membership id is required")
    val externalMembershipId: String = "",
    @field:NotBlank(message = "Member id is required")
    val memberId: String = "",
    val status: String = "ACTIVE"
)

data class PartnerMembershipResponse(
    val id: Long,
    val tenantId: Long,
    val sponsorId: Long,
    val externalMembershipId: String,
    val memberId: Long,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: PartnerMembershipEntity): PartnerMembershipResponse = PartnerMembershipResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            sponsorId = entity.sponsorId,
            externalMembershipId = entity.externalMembershipId,
            memberId = entity.memberId,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}