package com.codestorykh.account.dto.response;

import com.codestorykh.account.type.AmlRuleType;
import com.codestorykh.account.type.AmlSeverity;

public record AmlRuleHitDto(
        AmlRuleType ruleType,
        AmlSeverity severity,
        String description
) {}
