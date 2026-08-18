package com.reward.platform.api.controller

import com.reward.platform.api.entity.WalletHistoryEntity
import com.reward.platform.api.repository.MemberRepository
import com.reward.platform.api.repository.WalletHistoryRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api")
class WalletHistoryController(
    private val walletHistoryRepository: WalletHistoryRepository,
    private val memberRepository: MemberRepository
) {

    @GetMapping("/wallet-history/{tenantId}/{memberId}")
    fun getWalletHistory(
        @PathVariable tenantId: String,
        @PathVariable memberId: String
    ): ResponseEntity<List<WalletHistoryEntity>> {
        val member = memberRepository.findByTenantIdAndExternalUserId(tenantId, memberId)
        val lookupId = member?.id ?: memberId
        val results = walletHistoryRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, lookupId)
        if (results.isNotEmpty()) {
            return ResponseEntity.ok(results)
        }
        return ResponseEntity.ok(
            walletHistoryRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, memberId)
        )
    }
}