package com.codestorykh.payment.service;

import com.codestorykh.payment.dto.FundTransferRequest;
import com.codestorykh.payment.dto.FundTransferResponse;
import com.codestorykh.tracking.logging.util.TraceContext;
import com.codestorykh.tracking.logging.workflow.WorkflowTracker;
import com.codestorykh.tracking.logging.workflow.WorkflowTrackerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Demo fund-transfer pipeline using {@link WorkflowTracker}.
 */
@Service
public class FundTransferService {

    private static final Logger log = LoggerFactory.getLogger(FundTransferService.class);

    private final WorkflowTrackerFactory trackerFactory;

    public FundTransferService(WorkflowTrackerFactory trackerFactory) {
        this.trackerFactory = trackerFactory;
    }

    public FundTransferResponse transfer(FundTransferRequest request) {
        MDC.put("transactionId", request.transactionId());
        MDC.put("channel", "API");

        WorkflowTracker tracker = trackerFactory.create("FUND_TRANSFER", request.transactionId());

        try {
            tracker.runStep("VALIDATE_REQUEST", () -> {
                validate(request);
                return null;
            });

            CompletableFuture<Void> fraud = tracker.runStepAsync(
                    "FRAUD_SCREEN",
                    () -> CompletableFuture.runAsync(() -> simulateExternalCall("fraud", 40))
            );
            CompletableFuture<Void> limits = tracker.runStepAsync(
                    "LIMIT_CHECK",
                    () -> CompletableFuture.runAsync(() -> simulateExternalCall("limit", 30))
            );
            CompletableFuture.allOf(fraud, limits).join();

            tracker.runStep("CORE_DEBIT", () -> {
                simulateExternalCall("debit " + request.fromAccount(), 50);
                return null;
            });
            tracker.runStep("CORE_CREDIT", () -> {
                simulateExternalCall("credit " + request.toAccount(), 50);
                return null;
            });
            tracker.runStep("NOTIFY", () -> {
                log.info("Payment notification queued for {}", request.transactionId());
                return null;
            });

            return new FundTransferResponse(
                    request.transactionId(),
                    "SUCCESS",
                    "Fund transfer completed",
                    TraceContext.getTraceId()
            );
        } finally {
            log.info("Workflow summary:{}", tracker.getSummary().pretty());
            MDC.clear();
        }
    }

    private void validate(FundTransferRequest request) {
        if (request.fromAccount().equals(request.toAccount())) {
            throw new IllegalArgumentException("PAYMENT_INVALID: from and to accounts must differ");
        }
    }

    private void simulateExternalCall(String target, long sleepMs) {
        try {
            Thread.sleep(sleepMs);
            log.info("External call ok target={} thread={}", target, Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PAYMENT_INTERRUPTED: " + target, e);
        }
    }
}
