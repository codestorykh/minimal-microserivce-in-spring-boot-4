package com.codestorykh.account.type;

public enum AmlRuleType {
    // Onboarding / KYC
    UNDERAGE_CUSTOMER,
    INVALID_IDENTITY,
    WATCHLIST_IDENTITY,
    WATCHLIST_NAME,
    HIGH_RISK_PROFILE,

    // Transaction screening
    NAME_MISMATCH,
    HIGH_VALUE_TRANSACTION,
    ACCOUNT_STATUS_RESTRICTED
}
