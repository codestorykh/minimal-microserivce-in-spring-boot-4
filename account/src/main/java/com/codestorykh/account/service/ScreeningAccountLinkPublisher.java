package com.codestorykh.account.service;

import com.codestorykh.account.client.screening.AccountLinkedEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScreeningAccountLinkPublisher {

    private final KafkaTemplate<String, AccountLinkedEvent> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String topic;

    public ScreeningAccountLinkPublisher(
            @Autowired(required = false)
            @Qualifier("screeningAccountLinkedKafkaTemplate")
            KafkaTemplate<String, AccountLinkedEvent> kafkaTemplate,
            @Value("${screening.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${screening.kafka.account-link-topic:screening-account-link-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
        this.topic = topic;
    }

    @PostConstruct
    void logKafkaLinkStatus() {
        log.info("Screening account-link Kafka: enabled={} templatePresent={} topic={}",
                kafkaEnabled, kafkaTemplate != null, topic);
    }

    /**
     * Publishes account-link to Kafka after the surrounding DB transaction commits
     * so screening only updates accountNumber for successfully created accounts.
     */
    public void publishAfterCommit(AccountLinkedEvent event) {
        if (!kafkaEnabled) {
            log.warn("screening.kafka.enabled=false — skipping account-link publish screeningId={} accountNumber={}",
                    event.screeningId(), event.accountNumber());
            return;
        }

        if (kafkaTemplate == null) {
            throw new IllegalStateException(
                    "screening.kafka.enabled=true but KafkaTemplate bean screeningAccountLinkedKafkaTemplate is missing"
            );
        }

        Runnable send = () -> {
            try {
                var result = kafkaTemplate.send(topic, event.screeningId(), event)
                        .get(10, TimeUnit.SECONDS);
                var meta = result.getRecordMetadata();
                log.info(
                        "Published screening account-link topic={} partition={} offset={} screeningId={} accountNumber={}",
                        meta.topic(), meta.partition(), meta.offset(),
                        event.screeningId(), event.accountNumber()
                );
            } catch (Exception ex) {
                log.error(
                        "Failed to publish screening account-link topic={} screeningId={} accountNumber={}",
                        topic, event.screeningId(), event.accountNumber(), ex
                );
                throw new IllegalStateException("SCREENING_ACCOUNT_LINK_PUBLISH_FAILED: " + ex.getMessage(), ex);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
            log.info("Registered after-commit Kafka publish screeningId={} accountNumber={}",
                    event.screeningId(), event.accountNumber());
        } else {
            send.run();
        }
    }
}
