package com.reward.platform.api.repository

import com.reward.platform.api.entity.SystemUserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SystemUserRepository : JpaRepository<SystemUserEntity, Long> {
    fun findByEmailIgnoreCase(email: String): SystemUserEntity?
    fun findByUsernameIgnoreCase(username: String): SystemUserEntity?
    fun existsByUsernameIgnoreCase(username: String): Boolean
}