package com.codestorykh.money.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "tbl_fee_config")
public class FeeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_type", nullable = false, length = 40)
    private String serviceType;

    @Column(name = "fee_code", nullable = false, length = 40)
    private String feeCode;

    @Column(name = "flat_amount", precision = 18, scale = 4)
    private BigDecimal flatAmount;

    @Column(name = "percent_rate", precision = 10, scale = 6)
    private BigDecimal percentRate;

    @Column(nullable = false, length = 3)
    private String currency;

    protected FeeConfig() {
    }

    public FeeConfig(String serviceType, String feeCode, BigDecimal flatAmount, BigDecimal percentRate, String currency) {
        this.serviceType = serviceType;
        this.feeCode = feeCode;
        this.flatAmount = flatAmount;
        this.percentRate = percentRate;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getFeeCode() {
        return feeCode;
    }

    public BigDecimal getFlatAmount() {
        return flatAmount;
    }

    public BigDecimal getPercentRate() {
        return percentRate;
    }

    public String getCurrency() {
        return currency;
    }
}
