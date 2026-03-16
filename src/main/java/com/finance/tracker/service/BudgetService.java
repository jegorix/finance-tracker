package com.finance.tracker.service;

import com.finance.tracker.dto.request.BudgetPatchRequest;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;

import java.util.List;

public interface BudgetService {
    BudgetResponse findById(Long id);

    List<BudgetResponse> findAll();

    BudgetResponse create(BudgetRequest request);

    BudgetResponse update(Long id, BudgetRequest request);

    BudgetResponse patch(Long id, BudgetPatchRequest request);

    void delete(Long id);
}
