package com.reward.platform.core.domain

import java.math.BigDecimal
import java.time.Instant

data class Program(
    val id: String,
    val tenantId: String,
    val name: String,
    val currency: String,
    val timezone: String,
    val status: ProgramStatus = ProgramStatus.DRAFT,
    val createdAt: Instant = Instant.now(),
    val earningRate: BigDecimal = BigDecimal.ZERO,
    val redemptionRate: BigDecimal = BigDecimal.ZERO
)

enum class ProgramStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    ARCHIVED
}
