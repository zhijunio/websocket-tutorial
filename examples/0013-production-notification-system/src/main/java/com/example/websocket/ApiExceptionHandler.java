package com.example.websocket;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    Map<String, Object> badRequest(IllegalArgumentException exception) {
        return Map.of("status", HttpStatus.BAD_REQUEST.value(), "error", "bad_request", "message", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<Map<String, Object>> idempotencyConflict(IdempotencyConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", HttpStatus.CONFLICT.value(), "error", "idempotency_conflict", "message", exception.getMessage()));
    }
}
