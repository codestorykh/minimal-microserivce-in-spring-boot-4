package com.codestorykh.money.dto;

import com.codestorykh.money.domain.Money;

import java.math.BigDecimal;
import java.util.List;

public record PaymentCalculationResultDto(
    Money sourceAmount,
    Money targetAmount,
    BigDecimal appliedFxRate,
    List<FeeItemDto> fees,
    Money totalChargeToCustomerUSD
) {}