package com.reward.platform.api.repository

import com.reward.platform.api.entity.AccountEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.repository.query.Param

interface AccountRepository : JpaRepository<AccountEntity, Long> {
    fun findByTenantIdAndMemberIdAndAccountType(
        tenantId: Long,
        memberId: Long,
        accountType: String
    ): AccountEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLockedByTenantIdAndMemberIdAndAccountType(
        @Param("tenantId") tenantId: Long,
        @Param("memberId") memberId: Long,
        @Param("accountType") accountType: String
    ): AccountEntity?
}
