package com.codestorykh.account.service;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.AmlAlert;
import com.codestorykh.account.repository.AmlAlertRepository;
import com.codestorykh.account.repository.TransactionHistoryRepository;
import com.codestorykh.account.type.AccountStatus;
import com.codestorykh.account.type.AmlAlertStatus;
import com.codestorykh.account.type.AmlRuleType;
import com.codestorykh.account.type.AmlSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class InternalAmlInspectionService {

    private final AmlAlertRepository amlAlertRepository;
    private final TransactionHistoryRepository transactionRepository;

    // Threshold Definitions
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00"); // Single large tx
    private static final BigDecimal STRUCTURING_LOWER_BOUND = new BigDecimal("8000.00"); // Just below 10k
    private static final BigDecimal STRUCTURING_UPPER_BOUND = new BigDecimal("9999.99");
    private static final int STRUCTURING_TIME_WINDOW_HOURS = 24;
    private static final int STRUCTURING_MAX_COUNT = 3; // 3+ txs in 24 hours below threshold = Alert
    private static final int VELOCITY_MAX_COUNT = 5;    // 5+ txs in 5 minutes = Rapid movement
    private static final int VELOCITY_WINDOW_MINUTES = 5;

    public InternalAmlInspectionService(AmlAlertRepository amlAlertRepository,
                                        TransactionHistoryRepository transactionRepository) {
        this.amlAlertRepository = amlAlertRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void inspectTransaction(Account account, BigDecimal transactionAmount, String currency) {

        // RULE 1: High-Value Threshold Check (LCTR)
        checkHighValueThreshold(account, transactionAmount, currency);

        // RULE 2: Anti-Structuring Check ("Smurfing")
        checkStructuringBehavior(account, transactionAmount, currency);

        // RULE 3: Velocity Check (Rapid succession transfers)
        checkTransactionVelocity(account, transactionAmount, currency);

        // RULE 4: Sudden Activity on Inactive/Dormant Account
        checkDormantAccountActivity(account, transactionAmount, currency);
    }

    /**
     * Rule 1: Flags single transactions that meet or exceed $10,000 USD
     */
    private void checkHighValueThreshold(Account account, BigDecimal amount, String currency) {
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            createAlert(
                    account,
                    AmlRuleType.HIGH_VALUE,
                    AmlSeverity.HIGH,
                    amount,
                    currency,
                    String.format("Single transaction of %s %s exceeds regulatory reporting threshold of %s",
                            amount, currency, HIGH_VALUE_THRESHOLD)
            );
        }
    }

    /**
     * Rule 2: Detects multiple transactions intentionally structured just below $10,000 (e.g. $8,000–$9,999)
     */
    private void checkStructuringBehavior(Account account, BigDecimal amount, String currency) {
        if (amount.compareTo(STRUCTURING_LOWER_BOUND) >= 0 && amount.compareTo(STRUCTURING_UPPER_BOUND) <= 0) {

            Instant windowStart = Instant.now().minus(STRUCTURING_TIME_WINDOW_HOURS, ChronoUnit.HOURS);

            long recentCount = transactionRepository.countTransactionsInWindow(
                    account.getId(),
                    STRUCTURING_LOWER_BOUND,
                    STRUCTURING_UPPER_BOUND,
                    windowStart
            );

            if (recentCount >= STRUCTURING_MAX_COUNT) {
                createAlert(
                        account,
                        AmlRuleType.STRUCTURING,
                        AmlSeverity.CRITICAL,
                        amount,
                        currency,
                        String.format("Potential structuring detected: %d transactions between %s and %s within %d hours",
                                recentCount, STRUCTURING_LOWER_BOUND, STRUCTURING_UPPER_BOUND, STRUCTURING_TIME_WINDOW_HOURS)
                );
            }
        }
    }

    /**
     * Rule 3: Flags unusual high-frequency transactions within a narrow timeframe
     */
    private void checkTransactionVelocity(Account account, BigDecimal amount, String currency) {
        Instant windowStart = Instant.now().minus(VELOCITY_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long recentTxCount = transactionRepository.countTotalTransactionsSince(account.getId(), windowStart);

        if (recentTxCount >= VELOCITY_MAX_COUNT) {
            createAlert(
                    account,
                    AmlRuleType.HIGH_VALUE,
                    AmlSeverity.MEDIUM,
                    amount,
                    currency,
                    String.format("High transaction velocity: %d transactions executed within %d minutes",
                            recentTxCount, VELOCITY_WINDOW_MINUTES)
            );
        }
    }

    /**
     * Rule 4: Detects sudden large inflows or outflows on dormant or inoperative accounts
     */
    private void checkDormantAccountActivity(Account account, BigDecimal amount, String currency) {
        if (account.getStatus() == AccountStatus.DORMANT || account.getStatus() == AccountStatus.INOPERATIVE) {
            createAlert(
                    account,
                    AmlRuleType.DORMANT_ACTIVITY,
                    AmlSeverity.HIGH,
                    amount,
                    currency,
                    String.format("Transaction executed on account in %s state", account.getStatus())
            );
        }
    }

    private void createAlert(Account account, AmlRuleType ruleType, AmlSeverity severity, BigDecimal amount, String currency, String description) {
        log.warn("AML ALERT [{}] - Account: {} | Severity: {} | Details: {}",
                ruleType, account.getAccountNumber(), severity, description);

        AmlAlert alert = AmlAlert.builder()
                .account(account)
                .ruleType(ruleType)
                .severity(severity)
                .transactionAmount(amount)
                .currency(currency)
                .description(description)
                .status(AmlAlertStatus.PENDING)
                .build();

        amlAlertRepository.save(alert);

        // Auto-restrict critical risk triggers
        if (AmlSeverity.CRITICAL == severity) {
            account.setStatus(AccountStatus.FROZEN);
            log.error("ACCOUNT FROZEN AUTOMATICALLY due to CRITICAL AML Alert. Account Number: {}", account.getAccountNumber());
        }
    }
}