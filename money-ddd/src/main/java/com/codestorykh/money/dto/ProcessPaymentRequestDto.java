package com.codestorykh.money.dto;

import java.math.BigDecimal;

public record ProcessPaymentRequestDto(
        String senderAccountId,
        String receiverAccountId,
        BigDecimal sourceAmount,
        String sourceCurrency,
        BigDecimal targetAmount,
        String targetCurrency,
        double fxRate
) {}
