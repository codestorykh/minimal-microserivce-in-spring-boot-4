package com.codestorykh.screening.service.impl;

import com.codestorykh.screening.domain.RuleHit;
import com.codestorykh.screening.domain.ScreeningOutcome;
import com.codestorykh.screening.domain.onboarding.OnboardingScreeningAlert;
import com.codestorykh.screening.domain.onboarding.OnboardingScreeningContext;
import com.codestorykh.screening.domain.onboarding.OnboardingScreeningDomainService;
import com.codestorykh.screening.dto.event.AccountLinkedEvent;
import com.codestorykh.screening.dto.request.OnboardingCheckRequest;
import com.codestorykh.screening.dto.response.RuleHitDto;
import com.codestorykh.screening.dto.response.ScreeningResultDto;
import com.codestorykh.screening.repository.OnboardingScreeningAlertRepository;
import com.codestorykh.screening.service.OnboardingScreeningService;
import com.codestorykh.screening.type.AlertStatus;
import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;
import com.codestorykh.screening.watchlist.WatchlistScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Screening side of onboarding:
 * <ol>
 *   <li>{@link #inspectNewAccount} — save screening rows ({@code account_number} null)</li>
 *   <li>{@link #linkAccountToScreening} — Kafka consumer updates {@code account_number}</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingScreeningServiceImpl implements OnboardingScreeningService {

    private final OnboardingScreeningDomainService onboardingScreeningDomainService;
    private final WatchlistScreeningService watchlistScreeningService;
    private final OnboardingScreeningAlertRepository onboardingScreeningAlertRepository;

    @Override
    @Transactional
    public ScreeningResultDto inspectNewAccount(OnboardingCheckRequest request) {
        log.info("Onboarding check — persist screening rows for request {}", request);
        OnboardingScreeningContext context = OnboardingScreeningContext.from(request);
        String screeningId = onboardingScreeningDomainService.buildScreeningId(context);

        List<RuleHit> hits = new ArrayList<>(onboardingScreeningDomainService.evaluate(context));
        watchlistScreeningService.screenOnboarding(context.toWatchlistRequest()).stream()
                .map(RuleHit::fromDto)
                .forEach(hits::add);

        ScreeningResultDto result = ScreeningOutcome.toResult(
                screeningId,
                hits,
                "Onboarding screening completed"
        );

        // Always save so Kafka can later update account_number for this screeningId
        persistScreeningRows(screeningId, context, result);

        log.info("Saved onboarding screening screeningId={} risk={} decision={}",
                screeningId, result.riskLevel(), result.decision());
        return result;
    }

    @Override
    @Transactional
    public boolean linkAccountToScreening(AccountLinkedEvent event) {
        if (event == null || event.screeningId() == null || event.accountNumber() == null) {
            throw new IllegalArgumentException("SCREENING_LINK_INVALID: screeningId and accountNumber are required");
        }

        List<OnboardingScreeningAlert> alerts =
                onboardingScreeningAlertRepository.findByScreeningId(event.screeningId());
        if (alerts.isEmpty()) {
            throw new IllegalArgumentException(
                    "SCREENING_LINK_NOT_FOUND: no screening rows for screeningId=" + event.screeningId()
                            + " — onboarding check must persist before account-link"
            );
        }

        boolean freezeRequired = false;
        for (OnboardingScreeningAlert alert : alerts) {
            alert.setAccountNumber(event.accountNumber());
            if (alert.getSeverity() == Severity.CRITICAL) {
                freezeRequired = true;
            }
        }
        onboardingScreeningAlertRepository.saveAll(alerts);

        log.info("Updated account_number on {} row(s) screeningId={} -> accountNumber={}",
                alerts.size(), event.screeningId(), event.accountNumber());
        return freezeRequired;
    }

    /**
     * Persist screening outcome. {@code account_number} stays null until Kafka account-link.
     * Clean screens get {@link RuleType#ONBOARDING_CLEAR} so there is always a row to update.
     */
    private void persistScreeningRows(
            String screeningId,
            OnboardingScreeningContext context,
            ScreeningResultDto result
    ) {
        List<RuleHitDto> hits = result.matchedRules();
        if (hits.isEmpty()) {
            onboardingScreeningAlertRepository.save(OnboardingScreeningAlert.builder()
                    .screeningId(screeningId)
                    .identityType(context.identityType())
                    .identityNumber(context.identityNumber())
                    .ruleType(RuleType.ONBOARDING_CLEAR)
                    .severity(Severity.LOW)
                    .description("Onboarding screening completed — no rules triggered")
                    .status(AlertStatus.CLEARED)
                    .build());
            log.info("Persisted ONBOARDING_CLEAR screeningId={} (account_number pending Kafka link)",
                    screeningId);
            return;
        }

        for (RuleHitDto hit : hits) {
            log.warn("ONBOARDING ALERT [{}] screeningId={} severity={} :: {}",
                    hit.ruleType(), screeningId, hit.severity(), hit.description());

            onboardingScreeningAlertRepository.save(OnboardingScreeningAlert.builder()
                    .screeningId(screeningId)
                    .identityType(context.identityType())
                    .identityNumber(context.identityNumber())
                    .ruleType(hit.ruleType())
                    .severity(hit.severity())
                    .description(hit.description())
                    .status(AlertStatus.PENDING)
                    .build());
        }
    }
}
