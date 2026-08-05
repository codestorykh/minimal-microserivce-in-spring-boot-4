package com.codestorykh.money.dto;

import java.math.BigDecimal;

public record PaymentRequestDto(
    String serviceType,
    String sourceCurrency,
    String targetCurrency,
    BigDecimal sourceAmount,
    String customerTier
) {}