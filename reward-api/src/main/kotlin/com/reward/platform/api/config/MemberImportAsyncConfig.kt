package com.reward.platform.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class MemberImportAsyncConfig {
    @Bean("memberImportExecutor")
    fun memberImportExecutor(): TaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 4
        queueCapacity = 20
        setThreadNamePrefix("member-import-")
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(60)
        initialize()
    }
}
