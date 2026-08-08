package com.codestorykh.account.aml.watchlist;

import com.codestorykh.account.type.IdentityType;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Source of watchlist data. Replace {@link InMemoryWatchlistProvider} with DB/API later without
 * changing screening callers.
 */
public interface WatchlistProvider {

    Optional<WatchlistHit> matchSanctionedName(String fullName);

    Optional<WatchlistHit> matchPepName(String fullName);

    /**
     * Strong identity match: National ID (NID) + date of birth.
     * ID alone is not enough — both must match a watchlist entry.
     */
    Optional<WatchlistHit> matchIdentity(IdentityType identityType, String identityNumber, LocalDate dateOfBirth);

    Optional<WatchlistHit> matchCountry(String countryCode);

    Optional<WatchlistHit> matchCity(String city);

    Optional<WatchlistHit> matchOrganization(String organization);

    Optional<WatchlistHit> matchAccountNumber(String accountNumber);

    Optional<WatchlistHit> matchEmailDomain(String email);

    Optional<WatchlistHit> matchAdverseMediaKeyword(String fullName);
}
