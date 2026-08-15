package com.codestorykh.payment.controller;

import com.codestorykh.payment.dto.FundTransferRequest;
import com.codestorykh.payment.dto.FundTransferResponse;
import com.codestorykh.payment.service.FundTransferService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class FundTransferController {

    private final FundTransferService fundTransferService;

    public FundTransferController(FundTransferService fundTransferService) {
        this.fundTransferService = fundTransferService;
    }

    @PostMapping(path = "/transfers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FundTransferResponse transfer(@Valid @RequestBody FundTransferRequest request) {
        return fundTransferService.transfer(request);
    }
}
