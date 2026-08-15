package com.codestorykh.tracking.logging.demo;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Demo DTOs / collaborators for {@link InstantFundTransferService}.
 * Replace with real domain services in a payment microservice.
 */
public final class DemoModels {

    private DemoModels() {
    }

    public record TransferRequest(
            String channel,
            String sourceAccount,
            String destAccount,
            BigDecimal amount,
            String currency
    ) {
    }

    public record TransferResponse(
            String transactionId,
            String postingId,
            String status,
            String message
    ) {
    }

    public interface AccountValidationService {
        void validateSourceAndDestination(String sourceAccount, String destAccount, BigDecimal amount);
    }

    public interface AmlScreeningService {
        CompletableFuture<Boolean> screenTransactionAsync(String transactionId, String sourceAccount, BigDecimal amount);
    }

    public interface CoreLedgerService {
        String placeHold(String sourceAccount, BigDecimal amount);

        String commitTransfer(String holdReference, String sourceAccount, String destAccount, BigDecimal amount);

        Void releaseHoldsIfAny(String transactionId);
    }

    public interface NotificationService {
        CompletableFuture<Void> notifyPartiesAsync(String transactionId, TransferRequest request);
    }
}
