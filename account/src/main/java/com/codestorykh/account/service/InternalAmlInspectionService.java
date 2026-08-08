package com.codestorykh.account.service;

import com.codestorykh.account.aml.watchlist.WatchlistScreeningService;
import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.AmlAlert;
import com.codestorykh.account.domain.Customer;
import com.codestorykh.account.dto.event.AmlAccountLinkedEvent;
import com.codestorykh.account.dto.request.AmlNewAccountCheckRequest;
import com.codestorykh.account.dto.request.AmlTransactionCheckRequest;
import com.codestorykh.account.dto.response.AmlInspectionResultDto;
import com.codestorykh.account.dto.response.AmlRuleHitDto;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.repository.AmlAlertRepository;
import com.codestorykh.account.type.AccountStatus;
import com.codestorykh.account.type.AmlAlertStatus;
import com.codestorykh.account.type.AmlDecision;
import com.codestorykh.account.type.AmlRiskLevel;
import com.codestorykh.account.type.AmlRuleType;
import com.codestorykh.account.type.AmlSeverity;
import com.codestorykh.account.type.Gender;
import com.codestorykh.account.type.IdentityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class InternalAmlInspectionService {

    private static final int MINIMUM_AGE = 18;
    private static final BigDecimal HIGH_VALUE_USD = new BigDecimal("10000.00");
    private static final BigDecimal HIGH_VALUE_KHR = new BigDecimal("40000000.00");
    private static final DateTimeFormatter SCREENING_DOB_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AmlAlertRepository amlAlertRepository;
    private final AccountRepository accountRepository;
    private final WatchlistScreeningService watchlistScreeningService;

    public InternalAmlInspectionService(
            AmlAlertRepository amlAlertRepository,
            AccountRepository accountRepository,
            WatchlistScreeningService watchlistScreeningService
    ) {
        this.amlAlertRepository = amlAlertRepository;
        this.accountRepository = accountRepository;
        this.watchlistScreeningService = watchlistScreeningService;
    }

    /**
     * Pre-onboarding AML screen (before account exists).
     * Watchlist checks are delegated to {@link WatchlistScreeningService}.
     */
    @Transactional
    public AmlInspectionResultDto inspectNewAccount(AmlNewAccountCheckRequest request) {
        // Deterministic id from client KYC: id_value + DOB(yyyyMMdd) e.g. 100902983 + 1995-09-02 → 10090298319950902
        String screeningId = buildOnboardingScreeningId(request.getIdValue(), request.getDateOfBirth());
        List<AmlRuleHitDto> hits = new ArrayList<>();

        checkAge(request.getDateOfBirth(), hits);
        checkIdentityFormat(request.getIdType(), request.getIdValue(), hits);
        checkHighRiskProfile(request.getGender(), request.getDateOfBirth(), request.getIdType(), hits);
        hits.addAll(watchlistScreeningService.screenOnboarding(request.toWatchlistRequest()));

        AmlInspectionResultDto result = buildResult(screeningId, hits, "Onboarding AML inspection completed");
        persistOnboardingHits(screeningId, request, result);

        log.info("Pre-onboarding AML screeningId={} risk={} decision={}",
                screeningId, result.riskLevel(), result.decision());
        return result;
    }

    /**
     * Called by Kafka consumer after account creation succeeds.
     */
    @Transactional
    public void linkAccountToScreening(AmlAccountLinkedEvent event) {
        if (event == null || event.screeningId() == null || event.accountNumber() == null) {
            throw new IllegalArgumentException("AML_LINK_INVALID: screeningId and accountNumber are required");
        }

        Account account = accountRepository.findAccountByAccountNumber(event.accountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ACCOUNT_NOT_FOUND: " + event.accountNumber()));

        List<AmlAlert> alerts = amlAlertRepository.findByScreeningId(event.screeningId());
        if (alerts.isEmpty()) {
            log.warn("No AML alerts found for screeningId={} — nothing to link", event.screeningId());
            return;
        }

        boolean hasCritical = false;
        for (AmlAlert alert : alerts) {
            alert.setAccount(account);
            alert.setAccountNumber(account.getAccountNumber());
            if (alert.getSeverity() == AmlSeverity.CRITICAL) {
                hasCritical = true;
            }
        }
        amlAlertRepository.saveAll(alerts);

        if (hasCritical) {
            account.setStatus(AccountStatus.FROZEN);
            accountRepository.save(account);
            log.error("Account {} frozen after linking CRITICAL pre-onboarding AML screening {}",
                    account.getAccountNumber(), event.screeningId());
        }

        log.info("Linked {} AML alert(s) screeningId={} -> accountNumber={}",
                alerts.size(), event.screeningId(), event.accountNumber());
    }

    /**
     * Transaction AML screen: account status/name/amount rules + watchlist screening.
     */
    @Transactional
    public AmlInspectionResultDto inspectTransaction(AmlTransactionCheckRequest request) {
        Account account = accountRepository.findAccountByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ACCOUNT_NOT_FOUND: " + request.accountNumber()));

        Customer customer = account.getCustomer();
        List<AmlRuleHitDto> hits = new ArrayList<>();

        checkAccountStatus(account, hits);
        checkNameMatch(customer, request.firstName(), request.lastName(), hits);
        checkHighValueTransaction(request.txnAmount(), request.txnCcy(), hits);
        hits.addAll(watchlistScreeningService.screenTransaction(request.toWatchlistRequest()));

        String screeningId = UUID.randomUUID().toString();
        AmlInspectionResultDto result = buildResult(
                screeningId,
                hits,
                "Transaction AML inspection completed for account " + account.getAccountNumber()
        );
        persistTransactionHits(account, screeningId, result, request.txnAmount(), normalizeCurrency(request.txnCcy()));

        if (result.decision() == AmlDecision.BLOCK) {
            account.setStatus(AccountStatus.FROZEN);
            accountRepository.save(account);
            log.error("Account {} frozen due to BLOCK AML decision on transaction", account.getAccountNumber());
        }

        return result;
    }

    private void checkAge(LocalDate dateOfBirth, List<AmlRuleHitDto> hits) {
        if (dateOfBirth == null) {
            hits.add(hit(AmlRuleType.UNDERAGE_CUSTOMER, AmlSeverity.CRITICAL,
                    "Date of birth is missing"));
            return;
        }
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < MINIMUM_AGE) {
            hits.add(hit(AmlRuleType.UNDERAGE_CUSTOMER, AmlSeverity.CRITICAL,
                    "Customer age " + age + " is below minimum " + MINIMUM_AGE));
        }
    }

    private void checkIdentityFormat(IdentityType idType, String idValue, List<AmlRuleHitDto> hits) {
        if (idType == null || idValue == null || idValue.isBlank()) {
            hits.add(hit(AmlRuleType.INVALID_IDENTITY, AmlSeverity.HIGH,
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
            hits.add(hit(AmlRuleType.INVALID_IDENTITY, AmlSeverity.HIGH,
                    "Identity value '" + value + "' does not match expected format for " + idType));
        }
    }

    private void checkHighRiskProfile(
            Gender gender,
            LocalDate dateOfBirth,
            IdentityType idType,
            List<AmlRuleHitDto> hits
    ) {
        if (gender == Gender.UNKNOWN) {
            hits.add(hit(AmlRuleType.HIGH_RISK_PROFILE, AmlSeverity.LOW,
                    "Gender is unspecified — elevated KYC review recommended"));
        }

        if (dateOfBirth != null) {
            int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
            if (age >= 18 && age <= 21 && idType == IdentityType.FAMILY_BOOK) {
                hits.add(hit(AmlRuleType.HIGH_RISK_PROFILE, AmlSeverity.MEDIUM,
                        "Young customer onboarded with Family Book only — enhanced due diligence"));
            }
        }
    }

    private void checkAccountStatus(Account account, List<AmlRuleHitDto> hits) {
        AccountStatus status = account.getStatus();
        if (status == AccountStatus.FROZEN
                || status == AccountStatus.CLOSED
                || status == AccountStatus.PENDING) {
            hits.add(hit(AmlRuleType.ACCOUNT_STATUS_RESTRICTED, AmlSeverity.CRITICAL,
                    "Account status " + status + " does not allow transactions"));
        } else if (status == AccountStatus.DORMANT || status == AccountStatus.INOPERATIVE) {
            hits.add(hit(AmlRuleType.ACCOUNT_STATUS_RESTRICTED, AmlSeverity.HIGH,
                    "Transaction attempted on " + status + " account"));
        }
    }

    private void checkNameMatch(
            Customer customer,
            String requestFirstName,
            String requestLastName,
            List<AmlRuleHitDto> hits
    ) {
        String expected = normalizeName(customer.getFirstName(), customer.getLastName());
        String actual = normalizeName(requestFirstName, requestLastName);

        if (!expected.equals(actual)) {
            hits.add(hit(AmlRuleType.NAME_MISMATCH, AmlSeverity.HIGH,
                    "Provided name '" + actual + "' does not match account holder '" + expected + "'"));
        }
    }

    private void checkHighValueTransaction(BigDecimal amount, String currency, List<AmlRuleHitDto> hits) {
        String ccy = normalizeCurrency(currency);
        BigDecimal threshold = "KHR".equals(ccy) ? HIGH_VALUE_KHR : HIGH_VALUE_USD;

        if (amount.compareTo(threshold) >= 0) {
            hits.add(hit(AmlRuleType.HIGH_VALUE_TRANSACTION, AmlSeverity.HIGH,
                    "Transaction " + amount + " " + ccy + " meets/exceeds threshold " + threshold + " " + ccy));
        }
    }

    private AmlInspectionResultDto buildResult(String screeningId, List<AmlRuleHitDto> hits, String summary) {
        AmlSeverity maxSeverity = hits.stream()
                .map(AmlRuleHitDto::severity)
                .max(Comparator.comparingInt(this::severityRank))
                .orElse(AmlSeverity.LOW);

        AmlRiskLevel riskLevel = toRiskLevel(maxSeverity, hits.isEmpty());
        AmlDecision decision = toDecision(riskLevel);

        String resultSummary = hits.isEmpty()
                ? summary + " — no rules triggered"
                : summary + " — " + hits.size() + " rule(s) triggered";

        return new AmlInspectionResultDto(screeningId, riskLevel, decision, List.copyOf(hits), resultSummary);
    }

    private void persistOnboardingHits(
            String screeningId,
            AmlNewAccountCheckRequest request,
            AmlInspectionResultDto result
    ) {
        for (AmlRuleHitDto hit : result.matchedRules()) {
            log.warn("AML ALERT [{}] screeningId={} severity={} :: {}",
                    hit.ruleType(), screeningId, hit.severity(), hit.description());

            amlAlertRepository.save(AmlAlert.builder()
                    .screeningId(screeningId)
                    .identityType(request.getIdType())
                    .identityNumber(request.getIdValue())
                    .ruleType(hit.ruleType())
                    .severity(hit.severity())
                    .description(hit.description())
                    .status(AmlAlertStatus.PENDING)
                    .build());
        }
    }

    private void persistTransactionHits(
            Account account,
            String screeningId,
            AmlInspectionResultDto result,
            BigDecimal txnAmount,
            String currency
    ) {
        for (AmlRuleHitDto hit : result.matchedRules()) {
            log.warn("AML ALERT [{}] account={} severity={} :: {}",
                    hit.ruleType(), account.getAccountNumber(), hit.severity(), hit.description());

            amlAlertRepository.save(AmlAlert.builder()
                    .account(account)
                    .accountNumber(account.getAccountNumber())
                    .screeningId(screeningId)
                    .ruleType(hit.ruleType())
                    .severity(hit.severity())
                    .transactionAmount(txnAmount)
                    .currency(currency)
                    .description(hit.description())
                    .status(AmlAlertStatus.PENDING)
                    .build());
        }
    }

    /**
     * Onboarding screening id = {@code id_value} + DOB as {@code yyyyMMdd}.
     * Example: id_value=100902983, date_of_birth=1995-09-02 → {@code 10090298319950902}.
     */
    static String buildOnboardingScreeningId(String idValue, LocalDate dateOfBirth) {
        String id = idValue == null ? "" : idValue.trim();
        if (id.isEmpty() || dateOfBirth == null) {
            throw new IllegalArgumentException("AML_SCREENING_ID_INVALID: id_value and date_of_birth are required");
        }
        return id + dateOfBirth.format(SCREENING_DOB_FORMAT);
    }

    private static AmlRuleHitDto hit(AmlRuleType type, AmlSeverity severity, String description) {
        return new AmlRuleHitDto(type, severity, description);
    }

    private int severityRank(AmlSeverity severity) {
        return switch (severity) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }

    private AmlRiskLevel toRiskLevel(AmlSeverity maxSeverity, boolean noHits) {
        if (noHits) {
            return AmlRiskLevel.LOW;
        }
        return switch (maxSeverity) {
            case LOW -> AmlRiskLevel.LOW;
            case MEDIUM -> AmlRiskLevel.MEDIUM;
            case HIGH -> AmlRiskLevel.HIGH;
            case CRITICAL -> AmlRiskLevel.CRITICAL;
        };
    }

    private AmlDecision toDecision(AmlRiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> AmlDecision.ALLOW;
            case MEDIUM, HIGH -> AmlDecision.REVIEW;
            case CRITICAL -> AmlDecision.BLOCK;
        };
    }

    private static String normalizeName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
