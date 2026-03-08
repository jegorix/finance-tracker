package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionPatchRequest;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    TransactionResponse findById(Long id);

    List<TransactionResponse> findAll(boolean withEntityGraph);

    List<TransactionResponse> findByDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime);

    TransactionResponse create(TransactionRequest request);

    TransactionResponse update(Long id, TransactionRequest request);
    TransactionResponse patch(Long id, TransactionPatchRequest request);

    void delete(Long id);
}
