package com.codestorykh.tracking.logging.util;

import com.codestorykh.tracking.logging.support.FailSafe;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Fail-safe helpers for the active OpenTelemetry span.
 */
public final class TraceContext {

    private static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("http.response.status_code");
    private static final AttributeKey<String> ERROR_CODE = AttributeKey.stringKey("app.error.code");
    private static final AttributeKey<Boolean> APP_ERROR = AttributeKey.booleanKey("app.error");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
    private static final String INVALID_TRACE = "00000000000000000000000000000000";
    private static final String INVALID_SPAN = "0000000000000000";

    private TraceContext() {
    }

    public static String getSpanId() {
        return FailSafe.get(() -> {
            String id = Span.current().getSpanContext().getSpanId();
            return id == null || id.isBlank() ? INVALID_SPAN : id;
        }, INVALID_SPAN);
    }

    public static String getTraceId() {
        return FailSafe.get(() -> {
            String id = Span.current().getSpanContext().getTraceId();
            return id == null || id.isBlank() ? INVALID_TRACE : id;
        }, INVALID_TRACE);
    }

    public static void recordHttpFailure(Throwable exception, int httpStatus, String errorCode) {
        FailSafe.run(() -> {
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
            span.setStatus(StatusCode.ERROR, code + " (HTTP " + httpStatus + ")");
        });
    }
}
