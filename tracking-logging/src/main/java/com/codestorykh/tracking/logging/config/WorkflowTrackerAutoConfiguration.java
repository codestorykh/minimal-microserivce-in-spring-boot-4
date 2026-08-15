package com.codestorykh.tracking.logging.config;

import com.codestorykh.tracking.logging.workflow.WorkflowTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link WorkflowTrackerFactory}.
 * <p>
 * Ordered after Micrometer registry auto-config. If no {@link MeterRegistry} bean exists yet,
 * falls back to {@link SimpleMeterRegistry} so consuming apps always get the factory.
 */
@AutoConfiguration(after = {
        MetricsAutoConfiguration.class,
        CompositeMeterRegistryAutoConfiguration.class
})
@EnableConfigurationProperties(WorkflowTrackerProperties.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(
        prefix = "management.workflow-tracker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WorkflowTrackerAutoConfiguration {

    @Bean
    @ConditionalOnClass(OpenTelemetry.class)
    @ConditionalOnBean(OpenTelemetry.class)
    @ConditionalOnMissingBean
    public OpenTelemetryAppenderInitializer openTelemetryAppenderInitializer(OpenTelemetry openTelemetry) {
        return new OpenTelemetryAppenderInitializer(openTelemetry);
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowTrackerFactory workflowTrackerFactory(
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            WorkflowTrackerProperties properties
    ) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
        return new WorkflowTrackerFactory(meterRegistry, properties);
    }
}
