package com.reward.platform.api.repository

import com.reward.platform.api.entity.AccountEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<AccountEntity, Long> {
    fun findByTenantIdAndMemberIdAndAccountType(
        tenantId: Long,
        memberId: Long,
        accountType: String
    ): AccountEntity?
}
