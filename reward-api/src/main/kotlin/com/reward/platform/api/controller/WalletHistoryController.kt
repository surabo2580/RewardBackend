package com.reward.platform.api.controller

import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.WalletHistoryRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class WalletHistoryController(
    private val walletHistoryRepository: WalletHistoryRepository
) {

    @GetMapping("/wallet-history/{tenantId}/{memberId}")
    fun getWalletHistory(
        @PathVariable tenantId: String,
        @PathVariable memberId: String
    ): ResponseEntity<List<WalletHistoryEntity>> {
        return ResponseEntity.ok(
            walletHistoryRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
        )
    }
}
