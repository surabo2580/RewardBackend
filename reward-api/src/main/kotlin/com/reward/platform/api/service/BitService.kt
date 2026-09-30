package com.reward.platform.api.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.reward.platform.api.entity.BitEntity
import com.reward.platform.api.entity.BitType
import com.reward.platform.api.repository.BitRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class BitCommand(
    val tenantId: Long,
    val memberId: Long,
    val bitType: BitType,
    val reference: String? = null,
    val programId: Long? = null,
    val bitSponsorId: Long? = null,
    val billingSponsorId: Long? = null,
    val bitSource: String? = null,
    val locationId: Long? = null,
    val branchId: Long? = null,
    val channel: String? = null,
    val status: String = "COMPLETED",
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val grossAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String? = null,
    val redemptionPointsDelta: Long = 0,
    val recognitionPointsDelta: Long = 0,
    val appliedPolicyId: Long? = null,
    val appliedOfferIds: Collection<Long> = emptyList(),
    val originalBitId: Long? = null,
    val description: String? = null,
    val payload: Map<String, Any?>? = null,
    val createdByUserId: Long? = null,
    val interactionAt: Instant = Instant.now()
)

/** Records every member touchpoint as a BIT; ledger rows reference the BIT via bitId. */
@Service
class BitService(
    private val bitRepository: BitRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository
) {
    private val json = jacksonObjectMapper()

    companion object {
        const val MAX_PAYLOAD_LENGTH = 8000
    }

    /** Idempotent on (tenant, reference, type): replays return the existing BIT. */
    @Transactional
    fun record(command: BitCommand): BitEntity {
        val reference = command.reference?.trim()?.ifBlank { null }?.take(255)
            ?: "${command.bitType.name}-${UUID.randomUUID()}"
        val existing = bitRepository.findByTenantIdAndBitReferenceAndBitType(command.tenantId, reference, command.bitType.name)

        val program = (command.programId?.let { programRepository.findById(it).orElse(null) }
            ?: programRepository.findByTenantIdOrderByCreatedAtDesc(command.tenantId).firstOrNull())
            ?.takeIf { it.tenantId == command.tenantId }
        val bitSponsorId = command.bitSponsorId ?: program?.let { hostSponsorId(command.tenantId, it.id) }
        val payload = command.payload?.takeIf { it.isNotEmpty() }?.let { json.writeValueAsString(it) }
        require(payload == null || payload.length <= MAX_PAYLOAD_LENGTH) { "BIT payload exceeds $MAX_PAYLOAD_LENGTH characters" }

        val candidate = BitEntity(
                tenantId = command.tenantId,
                programId = program?.id,
                bitReference = reference,
                bitType = command.bitType.name,
                bitCategory = command.bitType.category.name,
                memberId = command.memberId,
                bitSponsorId = bitSponsorId,
                billingSponsorId = command.billingSponsorId ?: bitSponsorId,
                locationId = command.locationId,
                branchId = command.branchId,
                channel = command.channel?.trim()?.uppercase()?.ifBlank { null }?.take(30) ?: "POS",
                status = command.status,
                errorCode = command.errorCode?.trim()?.take(50),
                errorMessage = command.errorMessage?.trim()?.take(2000),
                bitSource = (command.bitSource ?: command.channel)?.trim()?.uppercase()?.ifBlank { null }?.take(30),
                grossAmount = command.grossAmount,
                discountAmount = command.discountAmount,
                netAmount = command.grossAmount.subtract(command.discountAmount).max(BigDecimal.ZERO),
                currency = command.currency ?: program?.currency,
                redemptionPointsDelta = command.redemptionPointsDelta,
                recognitionPointsDelta = command.recognitionPointsDelta,
                appliedPolicyId = command.appliedPolicyId,
                appliedOfferIds = command.appliedOfferIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                originalBitId = command.originalBitId,
                description = command.description?.take(500),
                payload = payload,
                createdByUserId = command.createdByUserId,
                interactionAt = command.interactionAt
            )
        if (existing != null) {
            if (existing.status == "FAILED" && command.status != "FAILED") {
                return bitRepository.save(candidate.copy(id = existing.id, createdAt = existing.createdAt))
            }
            return existing
        }
        return bitRepository.save(candidate)
    }

    @Transactional
    fun recordFailed(command: BitCommand, errorCode: String, errorMessage: String): BitEntity =
        record(command.copy(status = "FAILED", errorCode = errorCode, errorMessage = errorMessage))

    @Transactional
    fun markReversed(tenantId: Long, bitId: Long, fullReversal: Boolean) {
        val bit = bitRepository.findByTenantIdAndId(tenantId, bitId) ?: return
        bitRepository.save(bit.copy(status = if (fullReversal) "REVERSED" else "PARTIALLY_REVERSED"))
    }

    fun parsePayload(bit: BitEntity): Map<String, Any?>? =
        bit.payload?.let { runCatching { json.readValue<Map<String, Any?>>(it) }.getOrNull() }

    /** Maps a free-form reward event type onto a BIT type. */
    fun bitTypeForEvent(eventType: String, amount: Long): BitType =
        BitType.entries.firstOrNull { it.name == eventType.uppercase() }
            ?: if (amount > 0) BitType.PURCHASE else BitType.EVENT

    private fun hostSponsorId(tenantId: Long, programId: Long): Long? =
        sponsorRepository.findByTenantIdAndProgramIdOrderByName(tenantId, programId)
            .let { sponsors -> sponsors.firstOrNull { it.sponsorType == "HOST" } ?: sponsors.firstOrNull() }
            ?.id
}
