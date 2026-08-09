package com.codestorykh.screening.kafka;

import com.codestorykh.screening.dto.event.AccountLinkedEvent;
import com.codestorykh.screening.service.OnboardingScreeningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Step 2 of onboarding: consume account-created event and set account_number on saved screening rows.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "screening.kafka.enabled", havingValue = "true")
public class AccountLinkListener {

    private final OnboardingScreeningService onboardingScreeningService;

    public AccountLinkListener(OnboardingScreeningService onboardingScreeningService) {
        this.onboardingScreeningService = onboardingScreeningService;
    }

    @KafkaListener(
            topics = "${screening.kafka.account-link-topic:screening-account-link-topic}",
            groupId = "${screening.kafka.group-id:screening-account-link-group}",
            containerFactory = "accountLinkedListenerContainerFactory"
    )
    public void onAccountLinked(AccountLinkedEvent event) {
        if (event == null) {
            log.error("Received null account-link event — check Kafka JSON deserialization");
            return;
        }
        log.info("Kafka account-link received screeningId={} accountNumber={}",
                event.screeningId(), event.accountNumber());
        onboardingScreeningService.linkAccountToScreening(event);
    }
}
