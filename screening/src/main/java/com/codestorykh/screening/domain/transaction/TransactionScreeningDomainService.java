package com.codestorykh.screening.domain.transaction;

import com.codestorykh.screening.domain.RuleHit;
import com.codestorykh.screening.type.AccountStatus;
import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure transaction screening rules (no Spring / persistence / watchlist).
 */
public class TransactionScreeningDomainService {

    private static final BigDecimal HIGH_VALUE_USD = new BigDecimal("10000.00");
    private static final BigDecimal HIGH_VALUE_KHR = new BigDecimal("40000000.00");

    public List<RuleHit> evaluate(TransactionScreeningContext context) {
        List<RuleHit> hits = new ArrayList<>();
        checkAccountStatus(context.accountStatus(), hits);
        checkNameMatch(
                context.holderFirstName(),
                context.holderLastName(),
                context.firstName(),
                context.lastName(),
                hits
        );
        checkHighValueTransaction(context.txnAmount(), context.txnCcy(), hits);
        return hits;
    }

    private void checkAccountStatus(AccountStatus status, List<RuleHit> hits) {
        if (status == AccountStatus.FROZEN
                || status == AccountStatus.CLOSED
                || status == AccountStatus.PENDING) {
            hits.add(new RuleHit(
                    RuleType.ACCOUNT_STATUS_RESTRICTED,
                    Severity.CRITICAL,
                    "Account status " + status + " does not allow transactions"
            ));
        } else if (status == AccountStatus.DORMANT || status == AccountStatus.INOPERATIVE) {
            hits.add(new RuleHit(
                    RuleType.ACCOUNT_STATUS_RESTRICTED,
                    Severity.HIGH,
                    "Transaction attempted on " + status + " account"
            ));
        }
    }

    private void checkNameMatch(
            String holderFirstName,
            String holderLastName,
            String requestFirstName,
            String requestLastName,
            List<RuleHit> hits
    ) {
        String expected = normalizeName(holderFirstName, holderLastName);
        String actual = normalizeName(requestFirstName, requestLastName);

        if (!expected.equals(actual)) {
            hits.add(new RuleHit(
                    RuleType.NAME_MISMATCH,
                    Severity.HIGH,
                    "Provided name '" + actual + "' does not match account holder '" + expected + "'"
            ));
        }
    }

    private void checkHighValueTransaction(BigDecimal amount, String currency, List<RuleHit> hits) {
        String ccy = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        BigDecimal threshold = "KHR".equals(ccy) ? HIGH_VALUE_KHR : HIGH_VALUE_USD;

        if (amount != null && amount.compareTo(threshold) >= 0) {
            hits.add(new RuleHit(
                    RuleType.HIGH_VALUE_TRANSACTION,
                    Severity.HIGH,
                    "Transaction " + amount + " " + ccy + " meets/exceeds threshold " + threshold + " " + ccy
            ));
        }
    }

    private static String normalizeName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
