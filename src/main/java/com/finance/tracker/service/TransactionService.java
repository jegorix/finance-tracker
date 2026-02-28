package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import java.util.List;

public interface TransactionService {

    TransactionResponse getById(Long id);

    List<TransactionResponse> getAll();

    List<TransactionResponse> getByCategory(String category, boolean useEntityGraph);

    TransactionResponse create(TransactionRequest request);

    TransactionResponse update(Long id, TransactionRequest request);

    void delete(Long id);

    void demonstratePartialSave(TransactionRequest request);

    void demonstrateRolledBackSave(TransactionRequest request);
}
