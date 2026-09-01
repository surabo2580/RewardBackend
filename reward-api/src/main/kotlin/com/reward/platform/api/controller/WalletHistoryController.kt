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
import org.springframework.web.bind.annotation.RequestAttribute

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api")
class WalletHistoryController(
    private val walletHistoryRepository: WalletHistoryRepository,
    private val memberRepository: MemberRepository
) {

    @GetMapping("/wallet-history/{tenantId}/{memberId}")
    fun getWalletHistory(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @PathVariable tenantId: Long,
        @PathVariable memberId: String
    ): ResponseEntity<List<WalletHistoryEntity>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match API key" }
        val member = memberRepository.findByTenantIdAndExternalUserId(tenantId, memberId)
        val results = member?.let {
            walletHistoryRepository.findByTenantIdAndMemberIdOrderByCreatedAtDesc(tenantId, it.id)
        } ?: emptyList()
        return ResponseEntity.ok(results)
    }
}