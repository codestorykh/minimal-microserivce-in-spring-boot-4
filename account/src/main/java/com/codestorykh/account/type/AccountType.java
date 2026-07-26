package com.codestorykh.account.type;

import lombok.Getter;

@Getter
public enum AccountType {
    SAVING("Saving Account"),
    CURRENT("Current Account"),
    FIXED_DEPOSIT("Term / Fixed Deposit"),
    LOAN("Loan Account"),
    OVERDRAFT("Overdraft Account");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }
}