package com.codestorykh.screening.domain.onboarding;

import com.codestorykh.screening.domain.Auditable;
import com.codestorykh.screening.type.AlertStatus;
import com.codestorykh.screening.type.IdentityType;
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

/**
 * Alert persisted from account-onboarding / KYC screening.
 * Account number is null until linked after account creation.
 */
@Entity
@Table(
        name = "tbl_onboarding_screening_alert",
        indexes = {
                @Index(name = "idx_onboarding_alert_status", columnList = "status"),
                @Index(name = "idx_onboarding_alert_screening", columnList = "screening_id"),
                @Index(name = "idx_onboarding_alert_account", columnList = "account_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingScreeningAlert extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "screening_id", length = 64, nullable = false)
    private String screeningId;

    /** Linked after account creation (Kafka/HTTP account-link). */
    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", length = 40)
    private IdentityType identityType;

    @Column(name = "identity_number", length = 50)
    private String identityNumber;

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
