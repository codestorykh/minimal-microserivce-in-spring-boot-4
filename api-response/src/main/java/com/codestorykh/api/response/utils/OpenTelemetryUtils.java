package com.codestorykh.api.response.utils;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Helpers for reading/writing the active OpenTelemetry span.
 * Important: {@code @ExceptionHandler} methods that return 4xx/5xx often leave the span
 * as OK/Unset, so Tempo "error" views miss them unless we mark the span explicitly.
 */
public final class OpenTelemetryUtils {

    private static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("http.response.status_code");
    private static final AttributeKey<String> ERROR_CODE = AttributeKey.stringKey("app.error.code");
    private static final AttributeKey<Boolean> APP_ERROR = AttributeKey.booleanKey("app.error");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    private OpenTelemetryUtils() {
    }

    public static String getSpanId() {
        return Span.current().getSpanContext().getSpanId();
    }

    public static String getTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }

    /**
     * Marks the current span so 4xx/5xx handled by exception handlers still appear in Tempo.
     *
     * @param exception optional root cause (recorded on the span)
     * @param httpStatus HTTP status returned to the client
     * @param errorCode  business/system error code
     */
    public static void recordHttpFailure(Throwable exception, int httpStatus, String errorCode) {
        Span span = Span.current();
        if (!span.getSpanContext().isValid()) {
            return;
        }

        String code = (errorCode == null || errorCode.isBlank()) ? "HTTP_" + httpStatus : errorCode;

        span.setAttribute(HTTP_STATUS, httpStatus);
        span.setAttribute(ERROR_CODE, code);
        span.setAttribute(APP_ERROR, true);

        if (exception != null) {
            span.setAttribute(ERROR_TYPE, exception.getClass().getName());
            span.recordException(exception);
        }

        // Force ERROR status for both client (4xx) and server (5xx) failures so Grafana
        // Tempo error filters and TraceQL `status=error` include these requests.
        span.setStatus(StatusCode.ERROR, code + " (HTTP " + httpStatus + ")");
    }
}
