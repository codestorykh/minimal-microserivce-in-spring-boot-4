package com.codestorykh.account.domain;

import com.codestorykh.account.type.TransactionStatus;
import com.codestorykh.account.type.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "tbl_transaction_history",
    indexes = {
        // Crucial for the AML count queries filtering by account_id and created_at
        @Index(name = "idx_tx_account_created", columnList = "account_id, created_at"),
        @Index(name = "idx_tx_reference", columnList = "transaction_reference")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "account_id", 
        nullable = false, 
        foreignKey = @ForeignKey(name = "fk_tx_account")
    )
    private Account account;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 50)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "counterparty_account", length = 34)
    private String counterpartyAccount;

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.SUCCESS;
}