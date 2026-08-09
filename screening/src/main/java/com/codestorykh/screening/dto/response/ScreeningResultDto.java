package com.codestorykh.screening.dto.response;

import com.codestorykh.screening.type.ScreeningDecision;
import com.codestorykh.screening.type.RiskLevel;

import java.util.List;

public record ScreeningResultDto(
        String screeningId,
        RiskLevel riskLevel,
        ScreeningDecision decision,
        List<RuleHitDto> matchedRules,
        String summary
) {}
