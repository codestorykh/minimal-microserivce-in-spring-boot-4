package com.codestorykh.money.dto;


import com.codestorykh.money.domain.Money;


public record FeeItemDto(
    String feeCode,
    String currencyBasis,
    Money feeAmount
) {}