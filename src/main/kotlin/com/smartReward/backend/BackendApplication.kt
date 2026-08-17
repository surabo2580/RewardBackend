package com.smartReward.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@ComponentScan(basePackages = ["com.smartReward.backend", "com.smartreward.backend"])
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}

@RestController
@CrossOrigin(origins = ["*"])
class HealthCheckController {
    @GetMapping("/api/health", "/health", "/businesses")
    fun ping(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "SmartReward Spring Boot Backend",
            "port" to 8080
        )
    }
}