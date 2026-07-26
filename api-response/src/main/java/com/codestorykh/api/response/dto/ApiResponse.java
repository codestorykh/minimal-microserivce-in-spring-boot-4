package com.codestorykh.api.response.dto;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    List<String> warnings,
    ErrorDetail error,
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation completed successfully");
    }

    public static <T> ApiResponse<T> successWithWarnings(T data, String message, List<String> warnings) {
        return new ApiResponse<>(true, "SUCCESS_WITH_WARNINGS", message, data, warnings, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String errorCode, String errorMessage, ErrorDetail details) {
        return new ApiResponse<>(false, errorCode, errorMessage, null, null, details, Instant.now());
    }
}