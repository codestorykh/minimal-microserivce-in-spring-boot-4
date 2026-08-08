package com.codestorykh.account.aml.watchlist;

import com.codestorykh.account.type.AmlSeverity;

public record WatchlistHit(
        WatchlistCategory category,
        AmlSeverity severity,
        String matchedValue,
        String reason
) {
    public static WatchlistHit of(WatchlistCategory category, String matchedValue, String reason) {
        return new WatchlistHit(category, category.defaultSeverity(), matchedValue, reason);
    }
}
