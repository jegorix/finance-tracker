package com.finance.tracker.service;

import com.finance.tracker.dto.response.TransactionResponse;
import java.util.List;


public interface TransactionService {

    TransactionResponse getById(Long id);
    List<TransactionResponse> getByCategory(String category);

}
