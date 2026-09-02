package com.reward.platform.api.controller

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.util.regex.Pattern

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val fields = exception.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf("error" to "Validation failed", "fields" to fields)
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBusinessValidation(exception: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(
            mapOf("error" to (exception.message ?: "Invalid request"))
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(
            mapOf("error" to (exception.message ?: "Validation failed"))
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidation(exception: HandlerMethodValidationException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(
            mapOf("error" to (exception.message ?: "Request validation failed"))
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(exception: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> {
        val rawMessage = exception.mostSpecificCause.message ?: "Malformed JSON request"
        val fieldName = extractFieldNameFromJacksonMessage(rawMessage)

        val friendlyMessage = when {
            rawMessage.contains("Cannot map `null` into type `long`", ignoreCase = true) -> {
                if (fieldName != null) {
                    "Invalid request body: '$fieldName' is required and must be a number."
                } else {
                    "Invalid request body: one or more required numeric fields are missing or null."
                }
            }
            rawMessage.contains("Cannot deserialize value of type", ignoreCase = true) -> {
                if (fieldName != null) {
                    "Invalid request body: '$fieldName' has the wrong data type."
                } else {
                    "Invalid request body: one or more fields have the wrong data type."
                }
            }
            else -> "Malformed JSON request"
        }

        return ResponseEntity.badRequest().body(
            mapOf("error" to friendlyMessage)
        )
    }

    private fun extractFieldNameFromJacksonMessage(rawMessage: String): String? {
        val matcher = Pattern.compile("\\[\\\"([^\\\"]+)\\\"\\]").matcher(rawMessage)
        var fieldName: String? = null
        while (matcher.find()) {
            fieldName = matcher.group(1)
        }
        return fieldName
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.internalServerError().body(
            mapOf("error" to (exception.message ?: exception.javaClass.simpleName))
        )
    }
}
