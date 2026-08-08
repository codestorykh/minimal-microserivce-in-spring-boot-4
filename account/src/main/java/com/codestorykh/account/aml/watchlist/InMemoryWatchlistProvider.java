package com.codestorykh.account.aml.watchlist;

import com.codestorykh.account.type.IdentityType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/**
 * Minimal in-memory watchlists for local/testing.
 * Swap this bean for a DB-backed or external sanctions provider in production.
 */
@Component
public class InMemoryWatchlistProvider implements WatchlistProvider {

    private static final Set<String> SANCTIONED_NAMES = Set.of(
            "DELETE JOJO",
            "RETURN JOJO"
    );

    private static final Set<String> PEP_NAMES = Set.of(
            "MINISTER SAMPLE",
            "VIP OFFICIAL"
    );

    /**
     * Sanctioned NID + DOB pairs (composite key {@code ID|yyyy-MM-dd}).
     * Matching requires both values — NID alone does not trigger a hit.
     */
    private static final Set<String> SANCTIONED_NID_DOB = Set.of(
            WatchlistNormalizer.identityDobKey("SANCTION001", LocalDate.of(1990, 1, 15)),
            WatchlistNormalizer.identityDobKey("BLOCKED999", LocalDate.of(1985, 6, 20)),
            // Example: re-test with the same NID/DOB style used in onboarding
            WatchlistNormalizer.identityDobKey("123456789", LocalDate.of(1995, 1, 15))
    );

    private static final Set<String> SANCTIONED_COUNTRIES = Set.of(
            "KP", // North Korea
            "IR"  // Iran
    );

    private static final Set<String> HIGH_RISK_CITIES = Set.of(
            "PYONGYANG",
            "TEHRAN"
    );

    private static final Set<String> SANCTIONED_ORGS = Set.of(
            "SHADOW TRADING CO",
            "BLOCKED EXPORT LTD"
    );

    private static final Set<String> BLOCKED_ACCOUNTS = Set.of(
            "ACC_BLOCKED_001",
            "ACC_SANCTION_999"
    );

    private static final Set<String> RISKY_EMAIL_DOMAINS = Set.of(
            "MAILINATOR.COM",
            "TEMPMAIL.COM"
    );

    private static final Set<String> ADVERSE_MEDIA_KEYWORDS = Set.of(
            "FRAUDSTER",
            "TERROR"
    );

    @Override
    public Optional<WatchlistHit> matchSanctionedName(String fullName) {
        String normalized = WatchlistNormalizer.text(fullName);
        if (normalized.isBlank() || !SANCTIONED_NAMES.contains(normalized)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.SANCTIONED_NAME,
                normalized,
                "Name matches sanctioned person watchlist"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchPepName(String fullName) {
        String normalized = WatchlistNormalizer.text(fullName);
        if (normalized.isBlank() || !PEP_NAMES.contains(normalized)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.PEP_NAME,
                normalized,
                "Name matches PEP (Politically Exposed Person) watchlist"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchIdentity(
            IdentityType identityType,
            String identityNumber,
            LocalDate dateOfBirth
    ) {
        // Strong match is NID + DOB (National ID). Other ID types are ignored here.
        if (identityType != IdentityType.NATIONAL_ID) {
            return Optional.empty();
        }
        String key = WatchlistNormalizer.identityDobKey(identityNumber, dateOfBirth);
        if (key.isBlank() || !SANCTIONED_NID_DOB.contains(key)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.SANCTIONED_IDENTITY,
                key,
                "NID + DOB matches sanctioned identity watchlist (" + key + ")"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchCountry(String countryCode) {
        String normalized = WatchlistNormalizer.text(countryCode);
        if (normalized.isBlank() || !SANCTIONED_COUNTRIES.contains(normalized)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.SANCTIONED_COUNTRY,
                normalized,
                "Country matches high-risk / sanctioned country watchlist"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchCity(String city) {
        String normalized = WatchlistNormalizer.text(city);
        if (normalized.isBlank() || !HIGH_RISK_CITIES.contains(normalized)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.HIGH_RISK_CITY,
                normalized,
                "City matches high-risk geographic watchlist"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchOrganization(String organization) {
        String normalized = WatchlistNormalizer.text(organization);
        if (normalized.isBlank() || !SANCTIONED_ORGS.contains(normalized)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.SANCTIONED_ORGANIZATION,
                normalized,
                "Organization matches sanctioned organization watchlist"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchAccountNumber(String accountNumber) {
        String normalized = WatchlistNormalizer.text(accountNumber);
        if (normalized.isBlank() || !BLOCKED_ACCOUNTS.contains(normalized)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.BLOCKED_ACCOUNT,
                normalized,
                "Account number matches blocked / mule account watchlist"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchEmailDomain(String email) {
        String domain = WatchlistNormalizer.emailDomain(email);
        if (domain.isBlank() || !RISKY_EMAIL_DOMAINS.contains(domain)) {
            return Optional.empty();
        }
        return Optional.of(WatchlistHit.of(
                WatchlistCategory.RISKY_EMAIL_DOMAIN,
                domain,
                "Email domain matches disposable / high-risk domain watchlist"
        ));
    }

    @Override
    public Optional<WatchlistHit> matchAdverseMediaKeyword(String fullName) {
        String normalized = WatchlistNormalizer.text(fullName);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return ADVERSE_MEDIA_KEYWORDS.stream()
                .filter(normalized::contains)
                .findFirst()
                .map(keyword -> WatchlistHit.of(
                        WatchlistCategory.ADVERSE_MEDIA_KEYWORD,
                        keyword,
                        "Name contains adverse-media keyword '" + keyword + "'"
                ));
    }
}
