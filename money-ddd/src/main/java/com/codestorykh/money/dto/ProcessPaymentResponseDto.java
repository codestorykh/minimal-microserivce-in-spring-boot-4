package com.codestorykh.money.dto;

import java.math.BigDecimal;

public record ProcessPaymentResponseDto(
        String status,
        String senderAccountId,
        BigDecimal senderBalance,
        String senderCurrency,
        String receiverAccountId,
        BigDecimal receiverBalance,
        String receiverCurrency
) {}
