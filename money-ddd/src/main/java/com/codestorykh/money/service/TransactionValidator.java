package com.codestorykh.money.service;


import com.codestorykh.money.dto.TransactionContext;

public interface TransactionValidator {
    /**
     * Executes validation logic. 
     * Throws IllegalArgumentException or Custom ValidationException if invalid.
     */
    void validate(TransactionContext context);
}