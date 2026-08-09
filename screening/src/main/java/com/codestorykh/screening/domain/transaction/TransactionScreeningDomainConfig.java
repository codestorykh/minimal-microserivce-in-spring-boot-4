package com.codestorykh.screening.domain.transaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionScreeningDomainConfig {

    @Bean
    TransactionScreeningDomainService transactionScreeningDomainService() {
        return new TransactionScreeningDomainService();
    }
}
