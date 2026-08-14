package com.smartReward.backend.repository

import com.smartReward.backend.model.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface WalletRepository : JpaRepository<Wallet, Long> {

    fun findByUserIdAndBusinessId(
        userId: String,
        businessId: String
    ): Wallet?
}