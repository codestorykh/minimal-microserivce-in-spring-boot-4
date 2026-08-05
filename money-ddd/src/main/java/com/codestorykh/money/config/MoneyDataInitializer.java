package com.codestorykh.money.config;

import com.codestorykh.money.domain.Account;
import com.codestorykh.money.domain.FxRate;
import com.codestorykh.money.repository.AccountRepository;
import com.codestorykh.money.repository.FxRateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class MoneyDataInitializer {

    @Bean
    CommandLineRunner seedMoneyData(AccountRepository accountRepository, FxRateRepository fxRateRepository) {
        return args -> {
            if (fxRateRepository.count() == 0) {
                fxRateRepository.save(new FxRate("USD", "KHR", new BigDecimal("4040.00")));
            }

            if (accountRepository.findByAccountNumber("ACC_USD_123").isEmpty()) {
                accountRepository.save(new Account("ACC_USD_123", "USD", new BigDecimal("1000.00")));
            }
            if (accountRepository.findByAccountNumber("ACC_KHR_456").isEmpty()) {
                accountRepository.save(new Account("ACC_KHR_456", "KHR", BigDecimal.ZERO));
            }
        };
    }
}
