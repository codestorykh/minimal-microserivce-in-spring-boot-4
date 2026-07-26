/*
package com.codestorykh.account.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

public record OnboardingEvent(String customerNumber, String accountNumber, String email) {}

@Service
public class CustomerServiceNotifier {

    private final KafkaTemplate<String, OnboardingEvent> kafkaTemplate;

    public CustomerServiceNotifier(KafkaTemplate<String, OnboardingEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOnboardedEvent(String customerNumber, String accountNumber, String email) {
        OnboardingEvent event = new OnboardingEvent(customerNumber, accountNumber, email);
        kafkaTemplate.send("customer-onboarding-topic", customerNumber, event);
    }
}*/
