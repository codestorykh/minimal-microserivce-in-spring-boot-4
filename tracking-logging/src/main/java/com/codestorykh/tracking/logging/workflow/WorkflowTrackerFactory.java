package com.codestorykh.tracking.logging.workflow;

import com.codestorykh.tracking.logging.config.WorkflowTrackerProperties;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Factory bean used by consuming services to create a per-transaction {@link WorkflowTracker}.
 */
public class WorkflowTrackerFactory {

    private final MeterRegistry meterRegistry;
    private final WorkflowTrackerProperties properties;

    public WorkflowTrackerFactory(MeterRegistry meterRegistry, WorkflowTrackerProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    public WorkflowTracker create(String workflowName, String workflowId) {
        return new WorkflowTracker(workflowName, workflowId, meterRegistry, properties);
    }
}
