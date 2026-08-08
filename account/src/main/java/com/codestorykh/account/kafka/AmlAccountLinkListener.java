package com.codestorykh.account.kafka;

import com.codestorykh.account.dto.event.AmlAccountLinkedEvent;
import com.codestorykh.account.service.InternalAmlInspectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "aml.kafka.enabled", havingValue = "true")
public class AmlAccountLinkListener {

    private final InternalAmlInspectionService amlInspectionService;

    public AmlAccountLinkListener(InternalAmlInspectionService amlInspectionService) {
        this.amlInspectionService = amlInspectionService;
    }

    @KafkaListener(
            topics = "${aml.kafka.account-link-topic:aml-account-link-topic}",
            groupId = "${aml.kafka.group-id:account-aml-link-group}"
    )
    public void onAccountLinked(AmlAccountLinkedEvent event) {
        log.info("Received AML account-link event screeningId={} accountNumber={}",
                event.screeningId(), event.accountNumber());
        amlInspectionService.linkAccountToScreening(event);
    }
}
