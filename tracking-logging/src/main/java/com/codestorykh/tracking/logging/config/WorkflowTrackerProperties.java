package com.codestorykh.tracking.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management.workflow-tracker")
public class WorkflowTrackerProperties {

    private boolean enabled = true;

    /**
     * Metric key prefix sent to Prometheus.
     */
    private String metricPrefix = "workflow.step.duration";

    /**
     * Whether to emit percentiles (p50, p95, p99) in Prometheus timers.
     */
    private boolean enablePercentiles = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getMetricPrefix() { return metricPrefix; }
    public void setMetricPrefix(String metricPrefix) { this.metricPrefix = metricPrefix; }

    public boolean isEnablePercentiles() { return enablePercentiles; }
    public void setEnablePercentiles(boolean enablePercentiles) { this.enablePercentiles = enablePercentiles; }
}