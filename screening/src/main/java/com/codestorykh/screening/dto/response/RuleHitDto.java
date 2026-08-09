package com.codestorykh.screening.dto.response;

import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;

public record RuleHitDto(
        RuleType ruleType,
        Severity severity,
        String description
) {}
