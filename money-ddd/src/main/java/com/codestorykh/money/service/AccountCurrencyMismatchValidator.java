package com.codestorykh.money.service;

import com.codestorykh.money.dto.TransactionContext;
import org.springframework.stereotype.Component;

@Component
    public class AccountCurrencyMismatchValidator implements TransactionValidator {

        @Override
        public void validate(TransactionContext context) {
            // 1. Check Sender Currency matches Sender Account in DB
            if (!context.sourceMoney().getCurrency().equalsIgnoreCase(context.senderAccountCurrencyDB())) {
                throw new IllegalArgumentException(String.format(
                        "SENDER_CURRENCY_MISMATCH: Requested %s does not match Sender Account DB Currency %s",
                        context.sourceMoney().getCurrency(), context.senderAccountCurrencyDB()
                ));
            }

            // 2. Check Receiver Currency matches Receiver Account in DB
            if (!context.targetMoney().getCurrency().equalsIgnoreCase(context.receiverAccountCurrencyDB())) {
                throw new IllegalArgumentException(String.format(
                        "RECEIVER_CURRENCY_MISMATCH: Requested %s does not match Receiver Account DB Currency %s",
                        context.targetMoney().getCurrency(), context.receiverAccountCurrencyDB()
                ));
            }
        }
    }
