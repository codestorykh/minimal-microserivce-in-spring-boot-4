package com.codestorykh.api.response.utils;

import io.opentelemetry.api.trace.Span;


public class OpenTelemetryUtils {

    private OpenTelemetryUtils() {
    }

    public static String getSpanId() {
        // AUTOMATION: Fetch the active distributed trace context directly from OpenTelemetry
        var currentSpan = Span.current().getSpanContext();
        return currentSpan.getSpanId();
    }

    public static String getTraceId() {
        // AUTOMATION: Fetch the active distributed trace context directly from OpenTelemetry
        var currentSpan = Span.current().getSpanContext();
        return currentSpan.getTraceId();
    }


}