package com.finance.tracker.service;

import java.math.BigDecimal;
import com.finance.tracker.dto.request.TransactionPatchRequest;
import com.finance.tracker.dto.request.TransactionSearchQueryMode;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.dto.response.TransactionSearchResult;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

public interface TransactionService {

    TransactionResponse findById(Long id);

    List<TransactionResponse> findAll(boolean withEntityGraph);

    List<TransactionResponse> findByDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime);

    TransactionSearchResult search(
            TransactionSearchQueryMode queryMode,
            String budgetName,
            String accountName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable);

    TransactionResponse create(TransactionRequest request);

    TransactionResponse update(Long id, TransactionRequest request);
    TransactionResponse patch(Long id, TransactionPatchRequest request);

    void delete(Long id);
}
