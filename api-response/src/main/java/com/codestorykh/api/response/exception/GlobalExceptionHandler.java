package com.codestorykh.api.response.exception;

import com.codestorykh.api.response.dto.ApiResponse;
import com.codestorykh.api.response.dto.ErrorDetail;
import com.codestorykh.tracking.logging.util.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name:unknown-service}")
    private String currentServiceName;

    // 🎯 1. SPECIFIC MATCH: Explicit Business Logic Failures
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        TraceContext.recordHttpFailure(ex, ex.getStatus().value(), ex.getErrorCode());

        ErrorDetail errorDetail = buildErrorDetail(request, ex.getCustomValidations());
        ApiResponse<Void> response = ApiResponse
                .failure(
                        ex.getErrorCode(),
                        ex.getMessage(),
                        errorDetail
                );
        return new ResponseEntity<>(response, ex.getStatus());
    }

    // 🎯 2. SPECIFIC MATCH: Automated DTO Payload Validations (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        TraceContext.recordHttpFailure(ex, HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED");

        Map<String, String> validations = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                validations.put(error.getField(), error.getDefaultMessage())
        );

        ErrorDetail errorDetail = buildErrorDetail(request, validations);
        ApiResponse<Void> response = ApiResponse
                .failure("VALIDATION_FAILED", "Invalid input parameters", errorDetail);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 🎯 3. COARSE MATCH: Database Integrity and SQL Violations
    // Catching standard Spring data exceptions, or falling back to raw JDBC/Hibernate wrappers via root cause analysis
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,  jakarta.persistence.PersistenceException.class})
    public ResponseEntity<ApiResponse<Void>> handleDatabaseIntegrityErrors(Exception ex, HttpServletRequest request) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String rootMessage = rootCause.getMessage();
        Map<String, String> validations = null;
        String errorMessage = "A database constraint integrity check failed.";
        String errorCode = "DATABASE_CONSTRAINT_VIOLATION";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (rootMessage != null) {
            String messageLower = rootMessage.toLowerCase();

            // Check for Not-Null Violations (SQLState 23502)
            if (messageLower.contains("violates not-null constraint") || messageLower.contains("null value in column")) {
                errorCode = "REQUIRED_FIELD_MISSING";
                errorMessage = "A required database data field was missing.";
                validations = new HashMap<>();

                String targetColumn = "unknown_field";
                if (rootMessage.contains("column \"")) {
                    int start = rootMessage.indexOf("column \"") + 8;
                    int end = rootMessage.indexOf("\"", start);
                    if (start > 7 && end > start) {
                        targetColumn = rootMessage.substring(start, end);
                    }
                }
                validations.put(targetColumn, "This database column cannot be null or empty.");
            }
            // Check for Unique Constraint/Duplicate Records
            else if (messageLower.contains("duplicate key") || messageLower.contains("violates unique constraint")) {
                errorMessage = "The record details already exist in the system database.";
                errorCode = "DUPLICATE_RECORD";
                status = HttpStatus.CONFLICT;
            }
        }

        TraceContext.recordHttpFailure(ex, status.value(), errorCode);

        ErrorDetail errorDetail = buildErrorDetail(request, validations);
        ApiResponse<Void> response = ApiResponse
                .failure(
                        errorCode,
                        errorMessage,
                        errorDetail
                );
        return new ResponseEntity<>(response, status);
    }

    // 🛑 4. SAFETY NET: Generic Global Catch-All Fallback
    // Any unexpected system error (NullPointerException, array mismatches, etc.) ends up here.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletRequest request) {
        TraceContext.recordHttpFailure(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR");

        ErrorDetail errorDetail = buildErrorDetail(request, null); // Generic safety errors pass a null validation map

        ApiResponse<Void> response = ApiResponse.failure(
                "INTERNAL_SERVER_ERROR",
                "An unexpected system exception occurred on the server.",
                errorDetail
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper method to eliminate duplicate tracking instantiation code blocks
    private ErrorDetail buildErrorDetail(HttpServletRequest request, Map<String, String> validations) {
        return new ErrorDetail(
                request.getRequestURI(),
                currentServiceName,
                TraceContext.getTraceId(),
                TraceContext.getSpanId(),
                validations
        );
    }
}