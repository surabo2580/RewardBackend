package com.smartReward.backend.api

import com.smartReward.backend.dto.EventRequest
import com.smartReward.backend.dto.EventResponse
import com.smartReward.backend.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@CrossOrigin(origins = ["*"])
class EventController(
    private val eventService: EventService
) {

    @PostMapping
    fun trackEvent(@RequestBody request: EventRequest): ResponseEntity<EventResponse> {
        val response = eventService.processEvent(request)
        return ResponseEntity.ok(response)
    }
}