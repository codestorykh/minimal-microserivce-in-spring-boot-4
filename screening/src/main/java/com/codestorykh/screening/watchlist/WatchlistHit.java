package com.codestorykh.screening.watchlist;

import com.codestorykh.screening.type.Severity;

public record WatchlistHit(
        WatchlistCategory category,
        Severity severity,
        String matchedValue,
        String reason
) {
    public static WatchlistHit of(WatchlistCategory category, String matchedValue, String reason) {
        return new WatchlistHit(category, category.defaultSeverity(), matchedValue, reason);
    }
}
