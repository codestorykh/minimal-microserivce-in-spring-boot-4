package com.codestorykh.screening.watchlist;

import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;

public enum WatchlistCategory {
    SANCTIONED_NAME(RuleType.WATCHLIST_NAME, Severity.CRITICAL),
    SANCTIONED_IDENTITY(RuleType.WATCHLIST_IDENTITY, Severity.CRITICAL),
    SANCTIONED_COUNTRY(RuleType.WATCHLIST_COUNTRY, Severity.CRITICAL),
    PEP_NAME(RuleType.WATCHLIST_PEP, Severity.HIGH),
    HIGH_RISK_CITY(RuleType.WATCHLIST_CITY, Severity.MEDIUM),
    SANCTIONED_ORGANIZATION(RuleType.WATCHLIST_ORGANIZATION, Severity.CRITICAL),
    BLOCKED_ACCOUNT(RuleType.WATCHLIST_ACCOUNT, Severity.CRITICAL),
    RISKY_EMAIL_DOMAIN(RuleType.WATCHLIST_EMAIL_DOMAIN, Severity.MEDIUM),
    ADVERSE_MEDIA_KEYWORD(RuleType.ADVERSE_MEDIA, Severity.HIGH);

    private final RuleType ruleType;
    private final Severity defaultSeverity;

    WatchlistCategory(RuleType ruleType, Severity defaultSeverity) {
        this.ruleType = ruleType;
        this.defaultSeverity = defaultSeverity;
    }

    public RuleType ruleType() {
        return ruleType;
    }

    public Severity defaultSeverity() {
        return defaultSeverity;
    }
}
