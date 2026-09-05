package com.reward.platform.api.service

import com.reward.platform.api.entity.MemberImportJobEntity
import com.reward.platform.api.repository.MemberImportJobRepository
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

@Service
class MemberImportWorker(
    private val jobRepository: MemberImportJobRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${member-import.chunk-size:5000}") private val chunkSize: Int,
    @Value("\${member-import.error-sample-size:50}") private val errorSampleSize: Int
) {
    @Async("memberImportExecutor")
    fun process(jobId: Long) {
        val queuedJob = jobRepository.findById(jobId).orElseThrow()
        val totalRecords = countRecords(Path.of(queuedJob.storagePath))
        var job = jobRepository.save(queuedJob.copy(status = "PROCESSING", totalRecords = totalRecords, startedAt = Instant.now()))
        val errorPath = Path.of("${job.storagePath}.errors.csv")
        var processed = 0L
        var imported = 0L
        var duplicates = 0L
        var failed = 0L
        val errorSamples = mutableListOf<String>()
        val rows = mutableListOf<MemberRow>()

        try {
            Files.newBufferedReader(Path.of(job.storagePath), StandardCharsets.UTF_8).use { reader ->
                Files.newBufferedWriter(errorPath, StandardCharsets.UTF_8).use { errorWriter ->
                    errorWriter.write("row_number,external_user_id,error")
                    errorWriter.newLine()
                    CSVParser.parse(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build()).use { parser ->
                        require(parser.headerMap.keys.containsAll(setOf("external_user_id", "email", "tier"))) {
                            "CSV headers must include external_user_id, email, and tier"
                        }
                        for (record in parser) {
                            processed++
                            val row = try {
                                parseRecord(record.get("external_user_id"), record.get("email"), record.get("tier"))
                            } catch (exception: Exception) {
                                failed++
                                writeError(errorWriter, processed + 1, record.get("external_user_id"), exception.message ?: "Invalid row")
                                if (errorSamples.size < errorSampleSize) errorSamples += "Row ${processed + 1}: ${exception.message}"
                                null
                            }
                            if (row != null) rows += row
                            if (rows.size >= chunkSize) {
                                val result = importChunk(job.tenantId, rows)
                                imported += result.imported
                                duplicates += result.duplicates
                                rows.clear()
                                job = checkpoint(job, processed, imported, duplicates, failed, errorSamples)
                            }
                        }
                    }
                    if (rows.isNotEmpty()) {
                        val result = importChunk(job.tenantId, rows)
                        imported += result.imported
                        duplicates += result.duplicates
                    }
                }
            }
            jobRepository.save(job.copy(
                status = "COMPLETED", totalRecords = processed, processedRecords = processed,
                importedRecords = imported, duplicateRecords = duplicates, failedRecords = failed,
                errorFilePath = if (failed > 0) errorPath.toString() else null,
                errorSummary = errorSamples.joinToString("\n").ifBlank { null }, completedAt = Instant.now()
            ))
        } catch (exception: Exception) {
            jobRepository.save(job.copy(
                status = "FAILED", totalRecords = processed, processedRecords = processed,
                importedRecords = imported, duplicateRecords = duplicates, failedRecords = failed,
                errorFilePath = if (Files.exists(errorPath)) errorPath.toString() else null,
                errorSummary = (errorSamples + (exception.message ?: "Import processing failed")).joinToString("\n").take(4000), completedAt = Instant.now()
            ))
        }
    }

    private fun checkpoint(job: MemberImportJobEntity, processed: Long, imported: Long, duplicates: Long, failed: Long, errors: List<String>): MemberImportJobEntity =
        jobRepository.save(job.copy(
            processedRecords = processed, importedRecords = imported,
            duplicateRecords = duplicates, failedRecords = failed, errorSummary = errors.joinToString("\n").ifBlank { null }
        ))

    private fun countRecords(path: Path): Long =
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
            CSVParser.parse(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()).use { parser ->
                require(parser.headerMap.keys.containsAll(setOf("external_user_id", "email", "tier"))) {
                    "CSV headers must include external_user_id, email, and tier"
                }
                parser.count().toLong()
            }
        }

    private fun importChunk(tenantId: Long, rows: List<MemberRow>): ChunkResult = transactionTemplate.execute {
        val inserted = jdbcTemplate.batchUpdate(
            """INSERT INTO reward_members (tenant_id, external_user_id, email, tier, created_at)
               VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
               ON CONFLICT (tenant_id, external_user_id) DO NOTHING""", object : BatchPreparedStatementSetter {
                override fun getBatchSize() = rows.size
                override fun setValues(statement: java.sql.PreparedStatement, index: Int) {
                    val row = rows[index]
                    statement.setLong(1, tenantId)
                    statement.setString(2, row.externalUserId)
                    statement.setString(3, row.email)
                    statement.setString(4, row.tier)
                }
            }
        ).count { it > 0 }.toLong()
        jdbcTemplate.batchUpdate(
            """INSERT INTO reward_accounts (tenant_id, member_id, account_type, available_points, pending_points, redeemed_points, lifetime_earned_points, updated_at)
               SELECT ?, member.id, account_type, 0, 0, 0, 0, CURRENT_TIMESTAMP
               FROM reward_members member
               CROSS JOIN (VALUES ('REDEMPTION'), ('RECOGNITION')) AS account(account_type)
               WHERE member.tenant_id = ? AND member.external_user_id = ?
               ON CONFLICT (tenant_id, member_id, account_type) DO NOTHING""", object : BatchPreparedStatementSetter {
                override fun getBatchSize() = rows.size
                override fun setValues(statement: java.sql.PreparedStatement, index: Int) {
                    statement.setLong(1, tenantId)
                    statement.setLong(2, tenantId)
                    statement.setString(3, rows[index].externalUserId)
                }
            }
        )
        ChunkResult(inserted, rows.size - inserted)
    } ?: ChunkResult(0, rows.size.toLong())

    private fun parseRecord(externalUserId: String?, email: String?, tier: String?): MemberRow {
        val normalizedId = externalUserId?.trim().orEmpty()
        require(normalizedId.isNotBlank()) { "external_user_id is required" }
        require(normalizedId.length <= 255) { "external_user_id exceeds 255 characters" }
        val normalizedEmail = email?.trim()?.ifBlank { null }
        require(normalizedEmail == null || EMAIL.matches(normalizedEmail)) { "email is invalid" }
        return MemberRow(normalizedId, normalizedEmail, tier?.trim()?.uppercase()?.ifBlank { "STANDARD" } ?: "STANDARD")
    }

    private fun writeError(writer: BufferedWriter, rowNumber: Long, externalUserId: String?, error: String) {
        writer.write("$rowNumber,${csv(externalUserId.orEmpty())},${csv(error)}")
        writer.newLine()
    }

    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""

    private data class MemberRow(val externalUserId: String, val email: String?, val tier: String)
    private data class ChunkResult(val imported: Long, val duplicates: Long)

    companion object {
        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
