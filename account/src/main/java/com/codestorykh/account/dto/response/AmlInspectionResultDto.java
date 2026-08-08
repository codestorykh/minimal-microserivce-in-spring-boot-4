package com.codestorykh.account.dto.response;

import com.codestorykh.account.type.AmlDecision;
import com.codestorykh.account.type.AmlRiskLevel;

import java.util.List;

public record AmlInspectionResultDto(
        AmlRiskLevel riskLevel,
        AmlDecision decision,
        List<AmlRuleHitDto> matchedRules,
        String summary
) {}
