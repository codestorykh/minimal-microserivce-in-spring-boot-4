package com.codestorykh.account.client.screening;

public record ScreeningResult(
        String screeningId,
        String riskLevel,
        String decision,
        String summary
) {
    public boolean isBlocked() {
        return "BLOCK".equalsIgnoreCase(decision);
    }
}
