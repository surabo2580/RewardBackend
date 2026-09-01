package com.reward.platform.api.repository

import com.reward.platform.api.entity.PasswordResetTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, Long> {
    fun findByTokenHashAndUsedAtIsNull(tokenHash: String): PasswordResetTokenEntity?
}