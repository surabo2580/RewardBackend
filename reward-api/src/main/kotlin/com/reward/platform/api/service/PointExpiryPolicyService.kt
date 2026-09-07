package com.reward.platform.api.service

import com.reward.platform.api.entity.ProgramEntity
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.Month
import java.time.ZoneOffset

@Service
class PointExpiryPolicyService {
    fun expiresAt(program: ProgramEntity, earnedAt: Instant): Instant? = when (program.expiryType.uppercase()) {
        "NEVER" -> null
        "FIXED" -> {
            val date = earnedAt.atZone(ZoneOffset.UTC)
            val year = if (date.month == Month.DECEMBER && date.dayOfMonth == 31) date.year + 1 else date.year
            java.time.LocalDate.of(year, Month.DECEMBER, 31).atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
        }
        else -> earnedAt.atZone(ZoneOffset.UTC).plusMonths(program.expiryMonths.toLong()).toInstant()
    }
}