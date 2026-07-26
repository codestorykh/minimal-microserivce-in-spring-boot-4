package com.codestorykh.account.service;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.InterestAccrualLedger;
import com.codestorykh.account.repository.AccountRepository;
import com.codestorykh.account.repository.InterestAccrualLedgerRepository;
import com.codestorykh.account.type.AccountStatus;
import com.codestorykh.account.type.AccountType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class InterestManagementService {

    private final AccountRepository accountRepository;
    private final InterestAccrualLedgerRepository ledgerRepository;

    private static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.0250"); // 2.50% p.a.
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    public InterestManagementService(AccountRepository accountRepository,
                                     InterestAccrualLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * 1. Daily End-Of-Day (EOD) Interest Calculation Job
     * Runs at 00:05 AM every day
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void calculateDailyInterest() {
        LocalDate today = LocalDate.now();

        List<Account> savingsAccounts = accountRepository
                .findAccountByAccountTypeAndStatus(AccountType.SAVING, AccountStatus.ACTIVE);

        for (Account account : savingsAccounts) {
            // Prevent duplicate accrual for the same day
            if (ledgerRepository.existsByAccountAndAccrualDate(account, today)) {
                continue;
            }

            BigDecimal balance = account.getAvailableBalance();
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                // Formula: (Closing Balance * Annual Rate) / 365
                BigDecimal dailyAccrual = balance.multiply(ANNUAL_INTEREST_RATE)
                        .divide(DAYS_IN_YEAR, 4, RoundingMode.HALF_UP);

                InterestAccrualLedger ledgerEntry = InterestAccrualLedger.builder()
                        .account(account)
                        .accrualDate(today)
                        .closingBalance(balance)
                        .interestRate(ANNUAL_INTEREST_RATE)
                        .dailyAccrual(dailyAccrual)
                        .isPosted(false)
                        .build();

                ledgerRepository.save(ledgerEntry);
            }
        }
    }

    /**
     * 2. Monthly Capitalization Job: Sums all unposted interest and posts to Main Account Balance
     * Runs on the 1st day of every month at 01:00 AM
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    @Transactional
    public void postMonthlyInterestToMainAccount() {
        List<Account> savingsAccounts = accountRepository
                .findAccountByAccountTypeAndStatus(AccountType.SAVING, AccountStatus.ACTIVE);

        for (Account account : savingsAccounts) {
            capitalizeInterestForAccount(account);
        }
    }

    /**
     * Capitalizes unposted interest for a specific account.
     * Can also be triggered on demand during account closure.
     */
    @Transactional
    public void capitalizeInterestForAccount(Account account) {
        List<InterestAccrualLedger> unpostedEntries = ledgerRepository
                .findByAccountAndIsPostedFalse(account);

        if (unpostedEntries.isEmpty()) {
            return;
        }

        // Sum total accumulated unposted interest
        BigDecimal totalInterestToPost = unpostedEntries.stream()
                .map(InterestAccrualLedger::getDailyAccrual)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP); // Rounded to 2 decimal places for account balance credit

        if (totalInterestToPost.compareTo(BigDecimal.ZERO) > 0) {
            // Credit total interest directly to the main account balance
            account.setBookBalance(account.getBookBalance().add(totalInterestToPost));
            accountRepository.save(account);

            // Mark all included ledger items as posted
            Instant now = Instant.now();
            unpostedEntries.forEach(entry -> {
                entry.setIsPosted(true);
                entry.setPostedAt(now);
            });

            ledgerRepository.saveAll(unpostedEntries);
        }
    }
}