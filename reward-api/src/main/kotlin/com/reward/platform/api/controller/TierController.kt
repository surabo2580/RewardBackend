package com.reward.platform.api.controller

import com.reward.platform.api.repository.TierRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class TierResponse(
    val id: Long,
    val tenantId: Long,
    val programId: Long,
    val name: String,
    val rank: Int,
    val thresholdPoints: Long,
    val multiplier: BigDecimal
)

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/tiers")
class TierController(private val tierRepository: TierRepository) {
    @GetMapping
    fun list(
        @RequestAttribute("tenantId") authenticatedTenantId: Long,
        @RequestParam tenantId: Long,
        @RequestParam programId: Long
    ): ResponseEntity<List<TierResponse>> {
        require(tenantId == authenticatedTenantId) { "Tenant does not match authenticated tenant" }
        return ResponseEntity.ok(
            tierRepository.findByTenantIdAndProgramIdOrderByRank(tenantId, programId).map {
                TierResponse(it.id, it.tenantId, it.programId, it.name, it.rank, it.thresholdPoints, it.multiplier)
            }
        )
    }
}
