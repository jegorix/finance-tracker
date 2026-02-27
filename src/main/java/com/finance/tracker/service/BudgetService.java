package com.finance.tracker.service;

import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;

import java.util.List;

public interface BudgetService {
    BudgetResponse getBudgetById(Long id);

    List<BudgetResponse> getAllBudgets();

    BudgetResponse createBudget(BudgetRequest request);

    BudgetResponse updateBudget(Long id, BudgetRequest request);

    void deleteBudget(Long id);
}
