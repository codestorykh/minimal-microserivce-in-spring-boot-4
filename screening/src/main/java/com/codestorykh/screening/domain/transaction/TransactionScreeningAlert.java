package com.codestorykh.screening.domain.transaction;

import com.codestorykh.screening.domain.Auditable;
import com.codestorykh.screening.type.AlertStatus;
import com.codestorykh.screening.type.RuleType;
import com.codestorykh.screening.type.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Alert persisted from transaction screening (amount/currency + account context).
 */
@Entity
@Table(
        name = "tbl_transaction_screening_alert",
        indexes = {
                @Index(name = "idx_txn_alert_status", columnList = "status"),
                @Index(name = "idx_txn_alert_screening", columnList = "screening_id"),
                @Index(name = "idx_txn_alert_account", columnList = "account_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionScreeningAlert extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "screening_id", length = 64, nullable = false)
    private String screeningId;

    @Column(name = "account_number", length = 34, nullable = false)
    private String accountNumber;

    @Column(name = "transaction_amount", precision = 18, scale = 4)
    private BigDecimal transactionAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.PENDING;
}
