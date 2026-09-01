package com.reward.platform.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class SelfServeRegisterRequest(
    @field:NotBlank
    val businessName: String = "",

    @field:NotBlank
    val slug: String = "",

    @field:Email
    @field:NotBlank
    val adminEmail: String = "",

    @field:Size(min = 8, max = 72)
    val adminPassword: String = "",

    @field:NotBlank
    val programName: String = "",

    val currency: String? = "INR",
    val timezone: String? = "Asia/Kolkata",
    val earningRate: BigDecimal = BigDecimal.TEN,
    val redemptionRate: BigDecimal = BigDecimal.ONE
)

data class SelfServeRegisterResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: SystemUserProfileResponse,
    val tenant: TenantResponse,
    val program: ProgramResponse,
    val hostSponsor: SponsorResponse?,
    val onboardingType: String = "SELF_SERVE"
)

data class EnterpriseInquiryRequest(
    @field:NotBlank
    val companyName: String = "",

    @field:NotBlank
    val contactName: String = "",

    @field:Email
    @field:NotBlank
    val contactEmail: String = "",

    val companySize: String? = null,

    @field:PositiveOrZero
    val expectedMonthlyMembers: Long? = null,

    @field:PositiveOrZero
    val expectedMonthlyTransactions: Long? = null,

    val notes: String? = null
)

data class EnterpriseInquiryResponse(
    val onboardingRequestId: Long,
    val status: String,
    val message: String
)

data class EnterpriseProvisionRequest(
    val onboardingRequestId: Long? = null,
    val customPricingModel: String? = null,
    val contractReference: String? = null,

    @field:Valid
    val tenant: TenantProvisionRequest = TenantProvisionRequest()
)

data class EnterpriseProvisionResponse(
    val onboardingRequestId: Long?,
    val customPricingModel: String?,
    val contractReference: String?,
    val provisioned: TenantProvisionResponse,
    val onboardingType: String = "ENTERPRISE_MANAGED"
)