package com.codestorykh.account.domain;

import com.codestorykh.account.type.AmlAlertStatus;
import com.codestorykh.account.type.AmlRuleType;
import com.codestorykh.account.type.AmlSeverity;
import com.codestorykh.account.type.IdentityType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "tbl_aml_alert",
    indexes = {
        @Index(name = "idx_aml_alert_status", columnList = "status"),
        @Index(name = "idx_aml_alert_account", columnList = "account_id"),
        @Index(name = "idx_aml_alert_screening", columnList = "screening_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlAlert extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nullable during pre-onboarding screening; linked later via Kafka after account creation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "account_id",
        nullable = true,
        foreignKey = @ForeignKey(name = "fk_aml_account")
    )
    private Account account;

    @Column(name = "screening_id", length = 64)
    private String screeningId;

    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", length = 40)
    private IdentityType identityType;

    @Column(name = "identity_number", length = 50)
    private String identityNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private AmlRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AmlSeverity severity;

    @Column(name = "transaction_amount", precision = 18, scale = 4)
    private BigDecimal transactionAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AmlAlertStatus status = AmlAlertStatus.PENDING;
}
