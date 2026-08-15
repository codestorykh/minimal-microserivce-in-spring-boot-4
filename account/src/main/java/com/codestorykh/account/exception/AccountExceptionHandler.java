package com.codestorykh.account.exception;

import com.codestorykh.tracking.logging.util.TraceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String errorCode = extractErrorCode(ex.getMessage());
        TraceContext.recordHttpFailure(ex, HttpStatus.BAD_REQUEST.value(), errorCode);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode", errorCode,
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        TraceContext.recordHttpFailure(ex, HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR");

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode", "VALIDATION_ERROR",
                "message", details
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
