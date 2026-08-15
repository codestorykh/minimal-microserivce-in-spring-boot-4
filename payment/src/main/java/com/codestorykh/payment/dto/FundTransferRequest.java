package com.codestorykh.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FundTransferRequest(
        @NotBlank String transactionId,
        @NotBlank String fromAccount,
        @NotBlank String toAccount,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency
) {
}
