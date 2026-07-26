package com.codestorykh.api.response.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;
    private final Map<String, String> customValidations;

    public BusinessException(String errorCode, String message, HttpStatus status, Map<String, String> customValidations) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.customValidations = customValidations;
    }

    public String getErrorCode() { return errorCode; }
    public HttpStatus getStatus() { return status; }
    public Map<String, String> getCustomValidations() { return customValidations; }
}