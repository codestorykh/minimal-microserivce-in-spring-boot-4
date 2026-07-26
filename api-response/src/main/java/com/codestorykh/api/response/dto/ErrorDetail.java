package com.codestorykh.api.response.dto;

import java.util.Map;

public record ErrorDetail(
        String path,
        String serviceName,
        String traceId,
        String spanId,
        Map<String, String> validations
) {}