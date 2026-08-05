package com.codestorykh.money.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode", extractErrorCode(ex.getMessage()),
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "errorCode", extractErrorCode(ex.getMessage()),
                "message", ex.getMessage()
        ));
    }

    private static String extractErrorCode(String message) {
        if (message == null || message.isBlank()) {
            return "UNKNOWN_ERROR";
        }
        int colon = message.indexOf(':');
        return colon > 0 ? message.substring(0, colon).trim() : message.trim();
    }
}
