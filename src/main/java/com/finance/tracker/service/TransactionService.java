package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    TransactionResponse getTransactionById(Long id);

    List<TransactionResponse> getAllTransactions(boolean withEntityGraph);

    List<TransactionResponse> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate);

    TransactionResponse createTransaction(TransactionRequest request);

    TransactionResponse updateTransaction(Long id, TransactionRequest request);

    void deleteTransaction(Long id);
}
