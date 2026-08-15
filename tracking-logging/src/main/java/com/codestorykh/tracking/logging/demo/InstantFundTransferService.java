package com.codestorykh.tracking.logging.demo;

import com.codestorykh.tracking.logging.demo.DemoModels.AccountValidationService;
import com.codestorykh.tracking.logging.demo.DemoModels.AmlScreeningService;
import com.codestorykh.tracking.logging.demo.DemoModels.CoreLedgerService;
import com.codestorykh.tracking.logging.demo.DemoModels.NotificationService;
import com.codestorykh.tracking.logging.demo.DemoModels.TransferRequest;
import com.codestorykh.tracking.logging.demo.DemoModels.TransferResponse;
import com.codestorykh.tracking.logging.workflow.WorkflowTracker;
import com.codestorykh.tracking.logging.workflow.WorkflowTrackerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Reference fund-transfer pipeline showing how a consuming service uses {@link WorkflowTracker}.
 * <p>
 * This class is documentation-by-example (not a Spring {@code @Service}).
 * Copy the pattern into your payment / transfer microservice.
 */
public class InstantFundTransferService {

    private static final Logger log = LoggerFactory.getLogger(InstantFundTransferService.class);

    private final WorkflowTrackerFactory trackerFactory;
    private final AccountValidationService accountService;
    private final AmlScreeningService amlService;
    private final CoreLedgerService ledgerService;
    private final NotificationService notificationService;

    public InstantFundTransferService(
            WorkflowTrackerFactory trackerFactory,
            AccountValidationService accountService,
            AmlScreeningService amlService,
            CoreLedgerService ledgerService,
            NotificationService notificationService
    ) {
        this.trackerFactory = trackerFactory;
        this.accountService = accountService;
        this.amlService = amlService;
        this.ledgerService = ledgerService;
        this.notificationService = notificationService;
    }

    public TransferResponse executeTransfer(TransferRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Establish MDC at the boundary (snapshotted into WorkflowTracker)
        MDC.put("transactionId", transactionId);
        MDC.put("channel", request.channel());
        MDC.put("sourceAccount", maskAccount(request.sourceAccount()));

        WorkflowTracker tracker = trackerFactory.create("INSTANT_FUND_TRANSFER", transactionId);

        try {
            log.info("Initiating instant fund transfer amount={} {}", request.amount(), request.currency());

            tracker.runStep("VALIDATE_ACCOUNTS_AND_BALANCE", () -> {
                accountService.validateSourceAndDestination(
                        request.sourceAccount(), request.destAccount(), request.amount());
                return null;
            });

            CompletableFuture<Boolean> amlCheck = tracker.runStepAsync(
                    "AML_FRAUD_SCREENING",
                    () -> amlService.screenTransactionAsync(
                            transactionId, request.sourceAccount(), request.amount())
            );

            if (!Boolean.TRUE.equals(amlCheck.join())) {
                throw new SecurityException("Transaction flagged by Risk & AML Engine");
            }

            String holdReference = tracker.runStep(
                    "HOLD_SOURCE_FUNDS",
                    () -> ledgerService.placeHold(request.sourceAccount(), request.amount())
            );

            String postingId = tracker.runStep(
                    "POST_CORE_LEDGER_TRANSFER",
                    () -> ledgerService.commitTransfer(
                            holdReference,
                            request.sourceAccount(),
                            request.destAccount(),
                            request.amount())
            );

            tracker.runStepAsync(
                    "SEND_TRANSACTION_NOTIFICATIONS",
                    () -> notificationService.notifyPartiesAsync(transactionId, request)
            );

            return new TransferResponse(transactionId, postingId, "SUCCESS", "Transfer processed successfully");

        } catch (RuntimeException ex) {
            log.error("Fund transfer failed for transactionId={}", transactionId, ex);
            tracker.runStep("ROLLBACK_FUNDS_HOLD", () -> ledgerService.releaseHoldsIfAny(transactionId));
            throw ex;
        } finally {
            log.info("Workflow Execution Summary:{}", tracker.getSummary().pretty());
            MDC.clear();
        }
    }

    private static String maskAccount(String accNo) {
        if (accNo == null || accNo.length() < 4) {
            return "****";
        }
        return "****" + accNo.substring(accNo.length() - 4);
    }
}
