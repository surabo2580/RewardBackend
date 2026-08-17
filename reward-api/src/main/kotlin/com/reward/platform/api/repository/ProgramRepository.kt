package com.reward.platform.api.repository

import com.reward.platform.api.entity.ProgramEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProgramRepository : JpaRepository<ProgramEntity, String>
