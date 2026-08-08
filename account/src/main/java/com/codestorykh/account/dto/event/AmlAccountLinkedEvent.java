package com.codestorykh.account.dto.event;

/**
 * Published after account creation succeeds so AML can attach the new account number
 * to a pre-onboarding screening result.
 */
public record AmlAccountLinkedEvent(
        String screeningId,
        String accountNumber
) {}
