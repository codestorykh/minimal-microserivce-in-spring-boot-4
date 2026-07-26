package com.codestorykh.account.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "tbl_interest_accrual_ledger",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_accrual_date", columnNames = {"account_id", "accrual_date"})
    },
    indexes = {
        @Index(name = "idx_interest_ledger_account_posted", columnList = "account_id, is_posted"),
        @Index(name = "idx_interest_ledger_date", columnList = "accrual_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestAccrualLedger extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "closing_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal closingBalance;

    @Column(name = "interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "daily_accrual", nullable = false, precision = 18, scale = 4)
    private BigDecimal dailyAccrual;

    @Builder.Default
    @Column(name = "is_posted", nullable = false)
    private Boolean isPosted = false;

    @Column(name = "posted_at")
    private Instant postedAt;

}