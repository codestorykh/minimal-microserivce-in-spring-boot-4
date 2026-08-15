package com.codestorykh.payment.dto;

public record FundTransferResponse(
        String transactionId,
        String status,
        String message,
        String traceId
) {
}
