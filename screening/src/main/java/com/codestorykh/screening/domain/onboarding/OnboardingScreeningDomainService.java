package com.codestorykh.screening.domain.onboarding;

import com.codestorykh.screening.domain.RuleHit;
import com.codestorykh.screening.type.Gender;
import com.codestorykh.screening.type.IdentityType;
import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pure onboarding / KYC screening rules (no Spring / persistence / watchlist).
 */
public class OnboardingScreeningDomainService {

    private static final int MINIMUM_AGE = 18;
    private static final DateTimeFormatter SCREENING_DOB_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<RuleHit> evaluate(OnboardingScreeningContext context) {
        List<RuleHit> hits = new ArrayList<>();
        checkAge(context.dateOfBirth(), hits);
        checkIdentityFormat(context.identityType(), context.identityNumber(), hits);
        checkHighRiskProfile(context.gender(), context.dateOfBirth(), context.identityType(), hits);
        return hits;
    }

    /**
     * Deterministic screening id = id_value + DOB(yyyyMMdd).
     */
    public String buildScreeningId(OnboardingScreeningContext context) {
        String id = context.identityNumber() == null ? "" : context.identityNumber().trim();
        if (id.isEmpty() || context.dateOfBirth() == null) {
            throw new IllegalArgumentException("SCREENING_ID_INVALID: id_value and date_of_birth are required");
        }
        return id + context.dateOfBirth().format(SCREENING_DOB_FORMAT);
    }

    private void checkAge(LocalDate dateOfBirth, List<RuleHit> hits) {
        if (dateOfBirth == null) {
            hits.add(new RuleHit(RuleType.UNDERAGE_CUSTOMER, Severity.CRITICAL,
                    "Date of birth is missing"));
            return;
        }
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < MINIMUM_AGE) {
            hits.add(new RuleHit(RuleType.UNDERAGE_CUSTOMER, Severity.CRITICAL,
                    "Customer age " + age + " is below minimum " + MINIMUM_AGE));
        }
    }

    private void checkIdentityFormat(IdentityType idType, String idValue, List<RuleHit> hits) {
        if (idType == null || idValue == null || idValue.isBlank()) {
            hits.add(new RuleHit(RuleType.INVALID_IDENTITY, Severity.HIGH,
                    "Identity type or identity value is missing"));
            return;
        }

        String value = idValue.trim();
        boolean valid = switch (idType) {
            case NATIONAL_ID -> Pattern.matches("\\d{9,12}", value);
            case PASSPORT -> Pattern.matches("(?i)[A-Z0-9]{6,12}", value);
            case DRIVING_LICENSE -> Pattern.matches("(?i)[A-Z0-9\\-]{6,20}", value);
            case FAMILY_BOOK -> Pattern.matches("(?i)[A-Z0-9\\-/]{5,30}", value);
            case TAX_ID -> Pattern.matches("(?i)[A-Z0-9]{6,20}", value);
        };

        if (!valid) {
            hits.add(new RuleHit(RuleType.INVALID_IDENTITY, Severity.HIGH,
                    "Identity value '" + value + "' does not match expected format for " + idType));
        }
    }

    private void checkHighRiskProfile(Gender gender, LocalDate dateOfBirth, IdentityType idType, List<RuleHit> hits) {
        if (gender == Gender.UNKNOWN) {
            hits.add(new RuleHit(RuleType.HIGH_RISK_PROFILE, Severity.LOW,
                    "Gender is unspecified — elevated KYC review recommended"));
        }

        if (dateOfBirth != null) {
            int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
            if (age >= 18 && age <= 21 && idType == IdentityType.FAMILY_BOOK) {
                hits.add(new RuleHit(RuleType.HIGH_RISK_PROFILE, Severity.MEDIUM,
                        "Young customer onboarded with Family Book only — enhanced due diligence"));
            }
        }
    }
}
