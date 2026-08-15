package com.codestorykh.tracking.logging.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Fail-safe execution: logging/metrics bugs must never break business transactions.
 */
public final class FailSafe {

    private static final Logger FALLBACK = LoggerFactory.getLogger("TRACKING_FAILSAFE");

    private FailSafe() {
    }

    public static void run(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (Throwable t) {
            FALLBACK.warn("tracking-logging suppressed error: {}", t.toString());
        }
    }

    public static <T> T get(Supplier<T> supplier, T fallback) {
        if (supplier == null) {
            return fallback;
        }
        try {
            return supplier.get();
        } catch (Throwable t) {
            FALLBACK.warn("tracking-logging suppressed error: {}", t.toString());
            return fallback;
        }
    }
}
