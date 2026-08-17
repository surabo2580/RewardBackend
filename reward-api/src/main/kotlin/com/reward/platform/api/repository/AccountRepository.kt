package com.reward.platform.api.repository

import com.reward.platform.api.entity.AccountEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<AccountEntity, String> {
    fun findByTenantIdAndMemberIdAndAccountType(
        tenantId: String,
        memberId: String,
        accountType: String
    ): AccountEntity?
}
