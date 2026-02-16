package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    TransactionResponse getTransactionById(Long id);

    List<TransactionResponse> getTransactionsByDateRange(final LocalDate startDate, final LocalDate endDate);

    TransactionResponse createTransaction(TransactionRequest request);

    TransactionResponse updateTransaction(Long id, TransactionRequest request);

    void deleteTransaction(Long id);
}
