package com.reward.platform.api.repository

import com.reward.platform.api.entity.SystemUserInviteTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SystemUserInviteTokenRepository : JpaRepository<SystemUserInviteTokenEntity, Long> {
    fun findByTokenHashAndUsedAtIsNull(tokenHash: String): SystemUserInviteTokenEntity?
}