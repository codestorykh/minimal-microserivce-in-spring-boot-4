package com.codestorykh.screening.domain;

import com.codestorykh.screening.dto.response.RuleHitDto;
import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;

/**
 * Domain representation of a triggered screening rule.
 */
public record RuleHit(
        RuleType ruleType,
        Severity severity,
        String description
) {
    public RuleHitDto toDto() {
        return new RuleHitDto(ruleType, severity, description);
    }

    public static RuleHit fromDto(RuleHitDto dto) {
        return new RuleHit(dto.ruleType(), dto.severity(), dto.description());
    }
}
