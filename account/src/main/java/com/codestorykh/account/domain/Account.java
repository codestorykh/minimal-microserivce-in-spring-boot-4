package com.codestorykh.account.domain;

import com.codestorykh.account.type.AccountStatus;
import com.codestorykh.account.type.AccountType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "tbl_account",
        indexes = {
                // 1. Foreign Key Index: Fast lookup of all accounts owned by a specific customer
                @Index(name = "idx_account_customer_id", columnList = "customer_id"),

                // 2. Account Number Index: Fast lookup for transaction processing and verification
                @Index(name = "idx_account_number", columnList = "account_number"),

                // 3. Status Index: Fast filtering for active vs. dormant/closed accounts
                @Index(name = "idx_account_status", columnList = "status"),

                // 4. Composite Index: Highly optimized for querying customer accounts by active status and currency
                @Index(name = "idx_account_customer_status_currency", columnList = "customer_id, status, currency")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency; // e.g., USD, KHR, EUR

    @Builder.Default
    @Column(name = "book_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal bookBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "hold_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal holdAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * Calculated transient field for real-time liquidity
     */
    public BigDecimal getAvailableBalance() {
        return this.bookBalance.subtract(this.holdAmount);
    }
}