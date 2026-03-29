package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionSearchCriteria;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.dto.response.TransactionSearchResult;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

public interface TransactionService {

    TransactionResponse findById(Long id);

    List<TransactionResponse> findAll(boolean withEntityGraph);

    List<TransactionResponse> findByDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime);

    TransactionSearchResult search(TransactionSearchCriteria criteria, Pageable pageable);

    TransactionResponse create(TransactionRequest request);

    List<TransactionResponse> createBulkTx(List<TransactionRequest> requests);

    List<TransactionResponse> createBulkNoTx(List<TransactionRequest> requests);

    TransactionResponse update(Long id, TransactionRequest request);
    TransactionResponse patch(Long id, TransactionUpdateRequest request);

    void delete(Long id);
}
