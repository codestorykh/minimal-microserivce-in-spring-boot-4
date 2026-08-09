package com.codestorykh.screening.domain.transaction;

import com.codestorykh.screening.dto.request.TransactionCheckRequest;
import com.codestorykh.screening.type.AccountStatus;
import com.codestorykh.screening.watchlist.WatchlistScreeningRequest;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Value object carrying everything needed to evaluate a transaction screen.
 */
public record TransactionScreeningContext(
        String accountNumber,
        AccountStatus accountStatus,
        String holderFirstName,
        String holderLastName,
        String firstName,
        String lastName,
        BigDecimal txnAmount,
        String txnCcy,
        String counterpartyCountry
) {
    public static TransactionScreeningContext from(TransactionCheckRequest request) {
        return new TransactionScreeningContext(
                request.accountNumber(),
                parseAccountStatus(request.accountStatus()),
                request.holderFirstName(),
                request.holderLastName(),
                request.firstName(),
                request.lastName(),
                request.txnAmount(),
                normalizeCurrency(request.txnCcy()),
                request.counterpartyCountry()
        );
    }

    public WatchlistScreeningRequest toWatchlistRequest() {
        return new WatchlistScreeningRequest(
                firstName,
                lastName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                accountNumber,
                counterpartyCountry
        );
    }

    private static AccountStatus parseAccountStatus(String accountStatus) {
        if (accountStatus == null || accountStatus.isBlank()) {
            throw new IllegalArgumentException("ACCOUNT_STATUS_INVALID: account status is required");
        }
        try {
            return AccountStatus.valueOf(accountStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("ACCOUNT_STATUS_INVALID: unknown status '" + accountStatus + "'");
        }
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
