package com.codestorykh.account.dto.request;

import com.codestorykh.account.aml.watchlist.WatchlistScreeningRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AmlTransactionCheckRequest(
        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Transaction amount is required")
        @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero")
        BigDecimal txnAmount,

        @NotBlank(message = "Transaction currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String txnCcy,

        /** Optional beneficiary / remitter country for geographic watchlist */
        String counterpartyCountry
) {
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
}
