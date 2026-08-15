package com.codestorykh.tracking.logging.workflow;

import com.codestorykh.tracking.logging.config.WorkflowTrackerProperties;
import com.codestorykh.tracking.logging.support.FailSafe;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Per-request (or per-transaction) workflow tracker.
 * <p>
 * Guarantees:
 * <ul>
 *   <li>Fail-safe telemetry — metric/log bugs never abort business work</li>
 *   <li>MDC snapshot at creation for correlation across async steps</li>
 *   <li>Thread-safe step collection for concurrent / async completions</li>
 * </ul>
 */
public class WorkflowTracker {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTracker.class);

    private final String workflowName;
    private final String workflowId;
    private final MeterRegistry meterRegistry;
    private final WorkflowTrackerProperties properties;
    private final long startTimeNanos;
    private final Queue<StepResult> steps = new ConcurrentLinkedQueue<>();
    private final Map<String, String> mdcContextSnapshot;

    private volatile String overallStatus = "SUCCESS";

    public WorkflowTracker(
            String workflowName,
            String workflowId,
            MeterRegistry meterRegistry,
            WorkflowTrackerProperties properties
    ) {
        this.workflowName = workflowName;
        this.workflowId = workflowId;
        this.meterRegistry = meterRegistry;
        this.properties = properties == null ? new WorkflowTrackerProperties() : properties;
        this.startTimeNanos = System.nanoTime();
        Map<String, String> copy = MDC.getCopyOfContextMap();
        this.mdcContextSnapshot = copy != null ? Map.copyOf(copy) : Map.of();
    }

    /**
     * Synchronous step execution. Business exceptions are rethrown after telemetry.
     */
    public <T> T runStep(String stepName, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String stepStatus = "SUCCESS";
        String errorCode = "NONE";

        try {
            return action.get();
        } catch (RuntimeException | Error e) {
            this.overallStatus = "FAILED";
            stepStatus = "FAILED";
            errorCode = sanitizeErrorCode(e);
            throw e;
        } catch (Exception e) {
            // Supplier cannot throw checked Exception, but keep for safety
            this.overallStatus = "FAILED";
            stepStatus = "FAILED";
            errorCode = sanitizeErrorCode(e);
            throw new RuntimeException(e);
        } finally {
            recordTelemetry(sample, stepName, stepStatus, errorCode);
        }
    }

    /**
     * Void synchronous step.
     */
    public void runStep(String stepName, Runnable action) {
        runStep(stepName, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Asynchronous step. MDC from workflow creation is restored on the completion thread
     * when recording telemetry (fail-safe).
     */
    public <T> CompletableFuture<T> runStepAsync(String stepName, Supplier<CompletableFuture<T>> asyncAction) {
        Timer.Sample sample = Timer.start(meterRegistry);

        CompletableFuture<T> future;
        try {
            future = asyncAction.get();
        } catch (RuntimeException | Error e) {
            this.overallStatus = "FAILED";
            recordTelemetry(sample, stepName, "FAILED", sanitizeErrorCode(e));
            throw e;
        }

        if (future == null) {
            this.overallStatus = "FAILED";
            IllegalStateException error = new IllegalStateException("async step returned null future: " + stepName);
            recordTelemetry(sample, stepName, "FAILED", sanitizeErrorCode(error));
            return CompletableFuture.failedFuture(error);
        }

        return future.whenComplete((result, throwable) -> {
            String stepStatus = (throwable == null) ? "SUCCESS" : "FAILED";
            String errorCode = (throwable == null) ? "NONE" : sanitizeErrorCode(throwable);
            if (throwable != null) {
                this.overallStatus = "FAILED";
            }
            withMdcSnapshot(() -> recordTelemetry(sample, stepName, stepStatus, errorCode));
        });
    }

    /**
     * Fail-safe telemetry: metric/log bugs NEVER crash business transactions.
     * {@link Timer.Sample#stop(Timer)} returns elapsed nanos.
     */
    private void recordTelemetry(Timer.Sample sample, String stepName, String stepStatus, String errorCode) {
        FailSafe.run(() -> {
            Timer.Builder timerBuilder = Timer.builder(resolveMetricName())
                    .tag("workflow", nullToUnknown(workflowName))
                    .tag("workflow_id", nullToUnknown(workflowId))
                    .tag("step", nullToUnknown(stepName))
                    .tag("status", nullToUnknown(stepStatus))
                    .tag("error_code", nullToUnknown(errorCode));

            if (properties.isEnablePercentiles()) {
                timerBuilder.publishPercentiles(0.5, 0.95, 0.99);
            }

            long elapsedNanos = sample.stop(timerBuilder.register(meterRegistry));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
            steps.add(new StepResult(stepName, stepStatus, durationMs, errorCode));

            log.info(
                    "workflow={} workflowId={} step={} status={} durationMs={} errorCode={} traceId={} channel={}",
                    workflowName,
                    workflowId,
                    stepName,
                    stepStatus,
                    durationMs,
                    errorCode,
                    mdcContextSnapshot.getOrDefault("traceId", "-"),
                    mdcContextSnapshot.getOrDefault("channel", mdcContextSnapshot.getOrDefault("channelId", "-"))
            );
        });
    }

    private String resolveMetricName() {
        String prefix = properties.getMetricPrefix();
        if (prefix == null || prefix.isBlank()) {
            return "banking.workflow.step.duration";
        }
        return prefix;
    }

    private void withMdcSnapshot(Runnable action) {
        FailSafe.run(() -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (!mdcContextSnapshot.isEmpty()) {
                    MDC.setContextMap(mdcContextSnapshot);
                }
                action.run();
            } finally {
                if (previous == null || previous.isEmpty()) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        });
    }

    private String sanitizeErrorCode(Throwable e) {
        if (e == null) {
            return "UNKNOWN_ERROR";
        }
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String name = root.getClass().getSimpleName();
        return name.isEmpty() ? "UNKNOWN_ERROR" : name;
    }

    private static String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    public WorkflowSummary getSummary() {
        long totalDurationMs = (System.nanoTime() - startTimeNanos) / 1_000_000L;
        List<StepResult> snapshot = List.copyOf(steps);
        return new WorkflowSummary(workflowName, workflowId, overallStatus, totalDurationMs, snapshot);
    }

    public Map<String, String> mdcSnapshot() {
        return Collections.unmodifiableMap(mdcContextSnapshot);
    }

    public record StepResult(String name, String status, long durationMs, String errorCode) {

        public String pretty(int index) {
            String icon = "FAILED".equalsIgnoreCase(status) ? "✗" : "✓";
            String err = (errorCode == null || "NONE".equalsIgnoreCase(errorCode)) ? "" : "  error=" + errorCode;
            return String.format("│  %s [%d] %-36s %-8s %6dms%s",
                    icon, index, nullSafe(name), nullSafe(status), durationMs, err);
        }

        @Override
        public String toString() {
            return pretty(0);
        }

        private static String nullSafe(String value) {
            return value == null ? "-" : value;
        }
    }

    public record WorkflowSummary(
            String workflow,
            String workflowId,
            String status,
            long totalDurationMs,
            List<StepResult> steps
    ) {

        /**
         * Multi-line, log-friendly summary for console / Loki.
         */
        public String pretty() {
            StringBuilder sb = new StringBuilder(256);
            String statusIcon = "FAILED".equalsIgnoreCase(status) ? "✗" : "✓";
            sb.append('\n');
            sb.append("┌─ Workflow Summary ").repeat("─", 44).append('\n');
            sb.append("│  ").append(statusIcon).append(" workflow   : ").append(nullSafe(workflow)).append('\n');
            sb.append("│    workflowId : ").append(nullSafe(workflowId)).append('\n');
            sb.append("│    status     : ").append(nullSafe(status)).append('\n');
            sb.append("│    totalMs    : ").append(totalDurationMs).append('\n');
            sb.append("├─ Steps (").append(steps == null ? 0 : steps.size()).append(") ")
                    .repeat("─", 48).append('\n');
            if (steps == null || steps.isEmpty()) {
                sb.append("│  (no steps recorded)\n");
            } else {
                int i = 1;
                for (StepResult step : steps) {
                    sb.append(step.pretty(i++)).append('\n');
                }
            }
            sb.append("└").repeat("─", 62);
            return sb.toString();
        }

        @Override
        public String toString() {
            return pretty();
        }

        private static String nullSafe(String value) {
            return value == null ? "-" : value;
        }
    }
}
