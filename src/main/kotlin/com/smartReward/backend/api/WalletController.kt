package com.smartReward.backend.api

import com.smartReward.backend.dto.WalletResponse
import com.smartReward.backend.service.WalletService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/wallet")
class WalletController(
    private val walletService: WalletService
) {

    @GetMapping("/{businessId}/{userId}")
    fun getWallet(
        @PathVariable businessId: String,
        @PathVariable userId: String
    ): WalletResponse {
        return walletService.getWallet(
            userId = userId,
            businessId = businessId
        )
    }
}