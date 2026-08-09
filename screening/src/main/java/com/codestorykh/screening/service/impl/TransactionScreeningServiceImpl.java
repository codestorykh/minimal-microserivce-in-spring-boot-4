package com.codestorykh.screening.service.impl;

import com.codestorykh.screening.domain.RuleHit;
import com.codestorykh.screening.domain.ScreeningOutcome;
import com.codestorykh.screening.domain.transaction.TransactionScreeningAlert;
import com.codestorykh.screening.domain.transaction.TransactionScreeningContext;
import com.codestorykh.screening.domain.transaction.TransactionScreeningDomainService;
import com.codestorykh.screening.dto.request.TransactionCheckRequest;
import com.codestorykh.screening.dto.response.RuleHitDto;
import com.codestorykh.screening.dto.response.ScreeningResultDto;
import com.codestorykh.screening.repository.TransactionScreeningAlertRepository;
import com.codestorykh.screening.service.TransactionScreeningService;
import com.codestorykh.screening.type.AlertStatus;
import com.codestorykh.screening.type.ScreeningDecision;
import com.codestorykh.screening.watchlist.WatchlistScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionScreeningServiceImpl implements TransactionScreeningService {

    private final TransactionScreeningDomainService transactionScreeningDomainService;
    private final WatchlistScreeningService watchlistScreeningService;
    private final TransactionScreeningAlertRepository transactionScreeningAlertRepository;

    @Override
    @Transactional
    public ScreeningResultDto inspectTransaction(TransactionCheckRequest request) {
        TransactionScreeningContext context = TransactionScreeningContext.from(request);

        List<RuleHit> hits = new ArrayList<>(transactionScreeningDomainService.evaluate(context));
        watchlistScreeningService.screenTransaction(context.toWatchlistRequest()).stream()
                .map(RuleHit::fromDto)
                .forEach(hits::add);

        String screeningId = UUID.randomUUID().toString();
        ScreeningResultDto result = ScreeningOutcome.toResult(
                screeningId,
                hits,
                "Transaction screening completed for account " + context.accountNumber()
        );

        persistHits(context, screeningId, result);

        if (result.decision() == ScreeningDecision.BLOCK) {
            log.warn("BLOCK transaction screening for account {} — account service should freeze if applicable",
                    context.accountNumber());
        }

        log.info("Transaction screeningId={} account={} risk={} decision={}",
                screeningId, context.accountNumber(), result.riskLevel(), result.decision());
        return result;
    }

    private void persistHits(
            TransactionScreeningContext context,
            String screeningId,
            ScreeningResultDto result
    ) {
        for (RuleHitDto hit : result.matchedRules()) {
            log.warn("TRANSACTION ALERT [{}] account={} severity={} :: {}",
                    hit.ruleType(), context.accountNumber(), hit.severity(), hit.description());

            transactionScreeningAlertRepository.save(TransactionScreeningAlert.builder()
                    .accountNumber(context.accountNumber())
                    .screeningId(screeningId)
                    .ruleType(hit.ruleType())
                    .severity(hit.severity())
                    .transactionAmount(context.txnAmount())
                    .currency(context.txnCcy())
                    .description(hit.description())
                    .status(AlertStatus.PENDING)
                    .build());
        }
    }
}
