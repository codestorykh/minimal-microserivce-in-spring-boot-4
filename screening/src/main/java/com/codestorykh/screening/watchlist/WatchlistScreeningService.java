package com.codestorykh.screening.watchlist;

import com.codestorykh.screening.dto.response.RuleHitDto;
import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Orchestrates watchlist checks for onboarding and transaction application services.
 */
@Service
public class WatchlistScreeningService {

    private final WatchlistProvider watchlistProvider;

    public WatchlistScreeningService(WatchlistProvider watchlistProvider) {
        this.watchlistProvider = watchlistProvider;
    }

    public List<RuleHitDto> screenOnboarding(WatchlistScreeningRequest request) {
        List<RuleHitDto> hits = new ArrayList<>();

        collect(hits, () -> watchlistProvider.matchSanctionedName(request.fullName()));
        collect(hits, () -> watchlistProvider.matchPepName(request.fullName()));
        collect(hits, () -> watchlistProvider.matchAdverseMediaKeyword(request.fullName()));
        collect(hits, () -> watchlistProvider.matchIdentity(
                request.identityType(),
                request.identityNumber(),
                request.dateOfBirth()
        ));
        collect(hits, () -> matchRequiredCountry(request.country()));
        collect(hits, () -> watchlistProvider.matchCountry(request.secondaryCountry()));
        collect(hits, () -> watchlistProvider.matchCountry(request.idIssuingCountry()));
        collect(hits, () -> watchlistProvider.matchCity(request.city()));
        collect(hits, () -> watchlistProvider.matchOrganization(request.organization()));
        collect(hits, () -> watchlistProvider.matchEmailDomain(request.email()));

        addCountryMismatchIfNeeded(request, hits);
        return hits;
    }

    public List<RuleHitDto> screenTransaction(WatchlistScreeningRequest request) {
        List<RuleHitDto> hits = new ArrayList<>();

        collect(hits, () -> watchlistProvider.matchAccountNumber(request.accountNumber()));
        collect(hits, () -> watchlistProvider.matchSanctionedName(request.fullName()));
        collect(hits, () -> watchlistProvider.matchPepName(request.fullName()));
        collect(hits, () -> watchlistProvider.matchAdverseMediaKeyword(request.fullName()));
        collect(hits, () -> watchlistProvider.matchCountry(request.counterpartyCountry()));

        return hits;
    }

    private Optional<WatchlistHit> matchRequiredCountry(String country) {
        if (country == null || country.isBlank()) {
            return Optional.of(new WatchlistHit(
                    WatchlistCategory.SANCTIONED_COUNTRY,
                    Severity.HIGH,
                    "",
                    "Country is missing — cannot complete geographic risk screening"
            ));
        }
        return watchlistProvider.matchCountry(country);
    }

    private void addCountryMismatchIfNeeded(WatchlistScreeningRequest request, List<RuleHitDto> hits) {
        String residence = WatchlistNormalizer.text(request.country());
        String issuing = WatchlistNormalizer.text(request.idIssuingCountry());
        if (residence.isBlank() || issuing.isBlank() || residence.equals(issuing)) {
            return;
        }
        hits.add(new RuleHitDto(
                RuleType.COUNTRY_MISMATCH,
                Severity.MEDIUM,
                "ID issuing country '" + issuing + "' differs from residence country '" + residence + "'"
        ));
    }

    private void collect(List<RuleHitDto> hits, Supplier<Optional<WatchlistHit>> matcher) {
        matcher.get()
                .filter(Objects::nonNull)
                .map(this::toRuleHit)
                .ifPresent(hits::add);
    }

    private RuleHitDto toRuleHit(WatchlistHit hit) {
        return new RuleHitDto(hit.category().ruleType(), hit.severity(), hit.reason());
    }
}
