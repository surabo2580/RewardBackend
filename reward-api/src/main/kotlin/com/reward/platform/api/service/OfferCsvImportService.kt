package com.reward.platform.api.service

import com.reward.platform.api.entity.OfferEntity
import com.reward.platform.api.entity.OfferVoucherEntity
import com.reward.platform.api.repository.OfferRepository
import com.reward.platform.api.repository.OfferVoucherRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.math.BigDecimal
import java.time.Instant

data class OfferImportSummary(val imported: Int, val failed: Int, val errors: List<String>)

@Service
class OfferCsvImportService(
    private val offerRepository: OfferRepository,
    private val offerVoucherRepository: OfferVoucherRepository
) {
    @Transactional
    fun importCampaigns(tenantId: Long, programId: Long, input: InputStream): OfferImportSummary {
        val errors = mutableListOf<String>()
        var imported = 0
        input.bufferedReader().useLines { lines ->
            lines.drop(1).forEachIndexed { index, row ->
                if (row.isBlank()) return@forEachIndexed
                try {
                    val values = parseCsv(row)
                    val code = values.required(0, "offer_code").uppercase()
                    val category = values.required(2, "category").uppercase()
                    require(category in setOf("AWARD", "REWARD", "PRIVILEGE", "DEAL")) { "Invalid category" }
                    val existing = offerRepository.findByTenantIdAndOfferCode(tenantId, code)
                    require(existing == null) { "Offer code already exists" }
                    offerRepository.save(OfferEntity(
                        tenantId = tenantId, programId = programId, offerCode = code, name = values.required(1, "name"), category = category,
                        status = "LAUNCHED", scope = "PROGRAM", offerType = if (category == "AWARD") "HYBRID" else "BONUS_POINTS",
                        multiplier = values.decimal(3, BigDecimal.ONE), bonusPoints = values.long(4), pointsRequired = values.long(5),
                        discountType = values.optional(6)?.uppercase(), discountValue = values.optional(7)?.toBigDecimalOrNull(),
                        startDate = Instant.parse(values.required(8, "start_date")), endDate = values.optional(9)?.let(Instant::parse) ?: Instant.parse("9999-12-31T23:59:59Z"),
                        minSpend = values.decimal(10, BigDecimal.ZERO), isActive = true
                    ))
                    imported++
                } catch (exception: Exception) { errors += "Row ${index + 2}: ${exception.message ?: "Invalid data"}" }
            }
        }
        return OfferImportSummary(imported, errors.size, errors)
    }

    @Transactional
    fun importVouchers(tenantId: Long, input: InputStream): OfferImportSummary {
        val errors = mutableListOf<String>()
        var imported = 0
        input.bufferedReader().useLines { lines ->
            lines.drop(1).forEachIndexed { index, row ->
                if (row.isBlank()) return@forEachIndexed
                try {
                    val values = parseCsv(row)
                    val offer = offerRepository.findByTenantIdAndOfferCode(tenantId, values.required(0, "offer_code").uppercase()) ?: error("Offer not found")
                    require(offer.category == "REWARD") { "Voucher inventory requires a REWARD offer" }
                    offerVoucherRepository.save(OfferVoucherEntity(tenantId = tenantId, offerId = offer.id, voucherCode = values.required(1, "voucher_code"), expiresAt = values.optional(2)?.let(Instant::parse)))
                    imported++
                } catch (exception: Exception) { errors += "Row ${index + 2}: ${exception.message ?: "Invalid data"}" }
            }
        }
        return OfferImportSummary(imported, errors.size, errors)
    }

    private fun parseCsv(row: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        row.forEach { character ->
            when { character == '"' -> quoted = !quoted; character == ',' && !quoted -> { values += current.toString().trim(); current.clear() }; else -> current.append(character) }
        }
        values += current.toString().trim()
        return values
    }
    private fun List<String>.optional(index: Int) = getOrNull(index)?.takeIf { it.isNotBlank() }
    private fun List<String>.required(index: Int, name: String) = optional(index) ?: error("$name is required")
    private fun List<String>.decimal(index: Int, default: BigDecimal) = optional(index)?.toBigDecimalOrNull() ?: default
    private fun List<String>.long(index: Int) = optional(index)?.toLongOrNull() ?: 0
}