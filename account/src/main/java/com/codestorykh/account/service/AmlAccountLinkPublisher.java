package com.codestorykh.account.service;

import com.codestorykh.account.dto.event.AmlAccountLinkedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes account-number link events after successful account creation.
 * Uses Kafka when enabled; otherwise applies the link synchronously as a fallback.
 */
@Slf4j
@Service
public class AmlAccountLinkPublisher {

    private final ObjectProvider<KafkaTemplate<String, AmlAccountLinkedEvent>> kafkaTemplateProvider;
    private final InternalAmlInspectionService amlInspectionService;
    private final boolean kafkaEnabled;
    private final String topic;

    public AmlAccountLinkPublisher(
            ObjectProvider<KafkaTemplate<String, AmlAccountLinkedEvent>> kafkaTemplateProvider,
            InternalAmlInspectionService amlInspectionService,
            @Value("${aml.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${aml.kafka.account-link-topic:aml-account-link-topic}") String topic
    ) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.amlInspectionService = amlInspectionService;
        this.kafkaEnabled = kafkaEnabled;
        this.topic = topic;
    }

    public void publish(AmlAccountLinkedEvent event) {
        KafkaTemplate<String, AmlAccountLinkedEvent> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        if (kafkaEnabled && kafkaTemplate != null) {
            kafkaTemplate.send(topic, event.screeningId(), event);
            log.info("Published AML account-link event to topic={} screeningId={} accountNumber={}",
                    topic, event.screeningId(), event.accountNumber());
            return;
        }

        log.info("Kafka AML link disabled/unavailable — linking synchronously screeningId={} accountNumber={}",
                event.screeningId(), event.accountNumber());
        amlInspectionService.linkAccountToScreening(event);
    }
}
