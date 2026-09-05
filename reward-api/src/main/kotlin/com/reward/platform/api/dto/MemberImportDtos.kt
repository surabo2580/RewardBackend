package com.reward.platform.api.dto

import com.reward.platform.api.entity.MemberImportJobEntity
import java.time.Instant

data class MemberImportJobResponse(
    val id: Long,
    val status: String,
    val fileName: String,
    val totalRecords: Long,
    val processedRecords: Long,
    val importedRecords: Long,
    val duplicateRecords: Long,
    val failedRecords: Long,
    val errorSummary: String?,
    val createdAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?
) {
    companion object {
        fun from(job: MemberImportJobEntity) = MemberImportJobResponse(
            job.id, job.status, job.fileName, job.totalRecords, job.processedRecords,
            job.importedRecords, job.duplicateRecords, job.failedRecords, job.errorSummary,
            job.createdAt, job.startedAt, job.completedAt
        )
    }
}
