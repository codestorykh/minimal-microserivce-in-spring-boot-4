package com.codestorykh.screening.watchlist;

import java.time.LocalDate;
import java.util.Locale;

public final class WatchlistNormalizer {

    private WatchlistNormalizer() {
    }

    public static String text(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    public static String fullName(String firstName, String lastName) {
        return text(((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim());
    }

    /**
     * Composite key for NID + DOB watchlist matching: {@code <ID>|<yyyy-MM-dd>}.
     */
    public static String identityDobKey(String identityNumber, LocalDate dateOfBirth) {
        String id = text(identityNumber);
        if (id.isBlank() || dateOfBirth == null) {
            return "";
        }
        return id + "|" + dateOfBirth;
    }

    public static String emailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return text(email.substring(email.indexOf('@') + 1));
    }
}
