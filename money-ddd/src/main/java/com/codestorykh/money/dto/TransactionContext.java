package com.codestorykh.money.dto;

import com.codestorykh.money.domain.Money;

public record TransactionContext(
        String senderAccountId,
        String receiverAccountId,
        String senderAccountCurrencyDB,   // e.g., "USD" from DB
        String receiverAccountCurrencyDB, // e.g., "KHR" from DB
        Money sourceMoney,                // Money(100.00, "USD")
        Money targetMoney,                // Money(404000, "KHR")
        double appliedFxRate              // 4040.00
) {}