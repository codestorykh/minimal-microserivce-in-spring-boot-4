package com.codestorykh.account.type;

import lombok.Getter;

@Getter
public enum AccountStatus {
    PENDING("Pending Activation"),
    ACTIVE("Active"),
    DORMANT("Dormant"),
    INOPERATIVE("Inoperative"),
    FROZEN("Frozen / Restricted"),
    CLOSED("Closed");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }
}