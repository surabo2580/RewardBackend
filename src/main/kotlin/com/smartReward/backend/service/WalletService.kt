package com.smartReward.backend.service

import com.smartReward.backend.dto.WalletResponse
import com.smartReward.backend.model.Wallet
import com.smartReward.backend.repository.WalletRepository
import org.springframework.stereotype.Service

@Service
class WalletService(
    private val walletRepository: WalletRepository
) {
    fun getWallet(userId: String, businessId: String): WalletResponse {
        val wallet = walletRepository
            .findByUserIdAndBusinessId(userId, businessId)
            ?: Wallet(
                userId = userId,
                businessId = businessId
            )

        return WalletResponse(
            wallet.availablePoints,
            wallet.pendingPoints
        )
    }
}