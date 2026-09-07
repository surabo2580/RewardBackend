package com.reward.platform.api.service

import com.reward.platform.api.entity.MemberImportJobEntity
import com.reward.platform.api.repository.MemberImportJobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class MemberImportService(
    private val jobRepository: MemberImportJobRepository,
    private val worker: MemberImportWorker,
    @Value("\${member-import.storage-directory}") private val storageDirectory: String
) {
    fun queue(tenantId: Long, file: MultipartFile): MemberImportJobEntity {
        require(!file.isEmpty) { "Select a non-empty CSV file" }
        require(file.originalFilename?.endsWith(".csv", ignoreCase = true) == true) { "Only CSV files are supported" }
        val directory = Path.of(storageDirectory)
        Files.createDirectories(directory)
        val storagePath = directory.resolve("${UUID.randomUUID()}.csv")
        val job = jobRepository.save(MemberImportJobEntity(
            tenantId = tenantId,
            fileName = file.originalFilename ?: "members.csv",
            storagePath = storagePath.toString()
        ))
        try {
            file.inputStream.use { Files.copy(it, storagePath, StandardCopyOption.REPLACE_EXISTING) }
            worker.process(job.id)
            return job
        } catch (exception: Exception) {
            Files.deleteIfExists(storagePath)
            jobRepository.save(job.copy(status = "FAILED", errorSummary = exception.message ?: "Unable to store import file"))
            throw exception
        }
    }
}
