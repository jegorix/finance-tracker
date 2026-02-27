package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    TransactionResponse getTransactionById(Long id, boolean withBudget, boolean withUser);

    List<TransactionResponse> getTransactionsByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            boolean withBudget,
            boolean withUser);

    TransactionResponse createTransaction(TransactionRequest request);

    TransactionResponse updateTransaction(Long id, TransactionRequest request);

    void deleteTransaction(Long id);
}
