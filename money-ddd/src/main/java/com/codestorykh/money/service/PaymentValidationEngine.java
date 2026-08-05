package com.codestorykh.money.service;

import com.codestorykh.money.dto.TransactionContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentValidationEngine {

    // Spring automatically injects all Beans implementing TransactionValidator
    private final List<TransactionValidator> validators;

    public PaymentValidationEngine(List<TransactionValidator> validators) {
        this.validators = validators;
    }

    public void validateTransaction(TransactionContext context) {
        // Run all validators sequentially
        for (TransactionValidator validator : validators) {
            validator.validate(context);
        }
    }
}