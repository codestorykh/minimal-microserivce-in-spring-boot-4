package com.codestorykh.money.dto;

import java.math.BigDecimal;

public record AccountBalanceDto(
        String accountNumber,
        String currency,
        BigDecimal balance
) {}
