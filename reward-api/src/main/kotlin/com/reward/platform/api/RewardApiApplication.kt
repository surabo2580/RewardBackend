package com.reward.platform.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = [
    "com.reward.platform"
])
class RewardApiApplication

fun main(args: Array<String>) {
    runApplication<RewardApiApplication>(*args)
}
