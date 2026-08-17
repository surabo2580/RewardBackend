package com.smartReward.backend.api

import com.smartReward.backend.dto.WalletResponse
import com.smartReward.backend.service.WalletService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/wallet")
@CrossOrigin(origins = ["*"]) // 👈 ADD THIS LINE
class WalletController(
    private val walletService: WalletService
) {
    @GetMapping("/{businessId}/{userId}")
    fun getWallet(
        @PathVariable businessId: String,
        @PathVariable userId: String
    ): ResponseEntity<WalletResponse> {
        return ResponseEntity.ok(walletService.getWallet(businessId, userId))
    }
}