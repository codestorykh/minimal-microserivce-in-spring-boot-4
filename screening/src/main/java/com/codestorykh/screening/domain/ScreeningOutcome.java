package com.codestorykh.screening.domain;

import com.codestorykh.screening.dto.response.RuleHitDto;
import com.codestorykh.screening.dto.response.ScreeningResultDto;
import com.codestorykh.screening.type.RiskLevel;
import com.codestorykh.screening.type.ScreeningDecision;
import com.codestorykh.screening.type.Severity;

import java.util.Comparator;
import java.util.List;

/**
 * Shared helper: rule hits → risk level, decision, and API result DTO.
 */
public final class ScreeningOutcome {

    private ScreeningOutcome() {
    }

    public static ScreeningResultDto toResult(String screeningId, List<RuleHit> hits, String summaryPrefix) {
        List<RuleHitDto> dtos = hits.stream().map(RuleHit::toDto).toList();
        Severity maxSeverity = hits.stream()
                .map(RuleHit::severity)
                .max(Comparator.comparingInt(ScreeningOutcome::severityRank))
                .orElse(Severity.LOW);

        RiskLevel riskLevel = toRiskLevel(maxSeverity, hits.isEmpty());
        ScreeningDecision decision = toDecision(riskLevel);

        String summary = hits.isEmpty()
                ? summaryPrefix + " — no rules triggered"
                : summaryPrefix + " — " + hits.size() + " rule(s) triggered";

        return new ScreeningResultDto(screeningId, riskLevel, decision, dtos, summary);
    }

    private static int severityRank(Severity severity) {
        return switch (severity) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }

    private static RiskLevel toRiskLevel(Severity maxSeverity, boolean noHits) {
        if (noHits) {
            return RiskLevel.LOW;
        }
        return switch (maxSeverity) {
            case LOW -> RiskLevel.LOW;
            case MEDIUM -> RiskLevel.MEDIUM;
            case HIGH -> RiskLevel.HIGH;
            case CRITICAL -> RiskLevel.CRITICAL;
        };
    }

    private static ScreeningDecision toDecision(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> ScreeningDecision.ALLOW;
            case MEDIUM, HIGH -> ScreeningDecision.REVIEW;
            case CRITICAL -> ScreeningDecision.BLOCK;
        };
    }
}
