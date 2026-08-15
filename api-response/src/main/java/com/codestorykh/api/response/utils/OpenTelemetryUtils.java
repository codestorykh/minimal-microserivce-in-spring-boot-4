package com.codestorykh.api.response.utils;

import com.codestorykh.tracking.logging.util.TraceContext;

/**
 * @deprecated use {@link TraceContext} from {@code tracking-logging}.
 */
@Deprecated(forRemoval = true, since = "0.0.1")
public final class OpenTelemetryUtils {

    private OpenTelemetryUtils() {
    }

    public static String getSpanId() {
        return TraceContext.getSpanId();
    }

    public static String getTraceId() {
        return TraceContext.getTraceId();
    }

    public static void recordHttpFailure(Throwable exception, int httpStatus, String errorCode) {
        TraceContext.recordHttpFailure(exception, httpStatus, errorCode);
    }
}
