package com.reward.platform.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "reward_member_import_jobs",
    indexes = [Index(name = "idx_reward_member_import_jobs_tenant_created", columnList = "tenant_id,created_at")]
)
data class MemberImportJobEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val tenantId: Long = 0,
    @Column(nullable = false) val status: String = "QUEUED",
    @Column(nullable = false) val fileName: String = "",
    @Column(nullable = false) val storagePath: String = "",
    @Column(nullable = false) val totalRecords: Long = 0,
    @Column(nullable = false) val processedRecords: Long = 0,
    @Column(nullable = false) val importedRecords: Long = 0,
    @Column(nullable = false) val duplicateRecords: Long = 0,
    @Column(nullable = false) val failedRecords: Long = 0,
    val errorFilePath: String? = null,
    @Column(length = 4000) val errorSummary: String? = null,
    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    val startedAt: Instant? = null,
    val completedAt: Instant? = null
)
