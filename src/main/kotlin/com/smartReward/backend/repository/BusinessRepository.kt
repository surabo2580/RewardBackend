package com.smartReward.backend.repository

import com.smartReward.backend.model.Business
import org.springframework.data.jpa.repository.JpaRepository

interface BusinessRepository : JpaRepository<Business, String>