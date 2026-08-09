package com.codestorykh.screening.service;

import com.codestorykh.screening.dto.request.TransactionCheckRequest;
import com.codestorykh.screening.dto.response.ScreeningResultDto;

public interface TransactionScreeningService {

    ScreeningResultDto inspectTransaction(TransactionCheckRequest request);
}
