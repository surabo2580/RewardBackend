package com.reward.platform.api.repository

import com.reward.platform.api.entity.OnboardingRequestEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OnboardingRequestRepository : JpaRepository<OnboardingRequestEntity, Long>