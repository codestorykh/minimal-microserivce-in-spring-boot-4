package com.codestorykh.account.aml.watchlist;

import com.codestorykh.account.type.AmlRuleType;
import com.codestorykh.account.type.AmlSeverity;

public enum WatchlistCategory {
    SANCTIONED_NAME(AmlRuleType.WATCHLIST_NAME, AmlSeverity.CRITICAL),
    SANCTIONED_IDENTITY(AmlRuleType.WATCHLIST_IDENTITY, AmlSeverity.CRITICAL),
    SANCTIONED_COUNTRY(AmlRuleType.WATCHLIST_COUNTRY, AmlSeverity.CRITICAL),
    PEP_NAME(AmlRuleType.WATCHLIST_PEP, AmlSeverity.HIGH),
    HIGH_RISK_CITY(AmlRuleType.WATCHLIST_CITY, AmlSeverity.MEDIUM),
    SANCTIONED_ORGANIZATION(AmlRuleType.WATCHLIST_ORGANIZATION, AmlSeverity.CRITICAL),
    BLOCKED_ACCOUNT(AmlRuleType.WATCHLIST_ACCOUNT, AmlSeverity.CRITICAL),
    RISKY_EMAIL_DOMAIN(AmlRuleType.WATCHLIST_EMAIL_DOMAIN, AmlSeverity.MEDIUM),
    ADVERSE_MEDIA_KEYWORD(AmlRuleType.ADVERSE_MEDIA, AmlSeverity.HIGH);

    private final AmlRuleType ruleType;
    private final AmlSeverity defaultSeverity;

    WatchlistCategory(AmlRuleType ruleType, AmlSeverity defaultSeverity) {
        this.ruleType = ruleType;
        this.defaultSeverity = defaultSeverity;
    }

    public AmlRuleType ruleType() {
        return ruleType;
    }

    public AmlSeverity defaultSeverity() {
        return defaultSeverity;
    }
}
