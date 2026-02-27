package com.finance.tracker.mapper;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget budget) {
        return toResponse(budget, true);
    }

    public BudgetResponse toResponse(Budget budget, boolean includeTransactions) {
        if (budget == null) {
            return null;
        }

        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setName(budget.getName());
        response.setLimitAmount(budget.getLimitAmount());
        response.setSpent(budget.getSpent());

        response.setCategoryIds(
                budget.getCategories() != null ? budget.getCategories().stream().map(Category::getId).toList() : null);

        if (includeTransactions) {
            response.setTransactionIds(
                    budget.getTransactions() != null
                            ? budget.getTransactions().stream().map(Transaction::getId).toList()
                            : null);
        } else {
            response.setTransactionIds(null);
        }

        return response;
    }

    public Budget fromRequest(BudgetRequest request, List<Category> categories) {
        if (request == null) {
            return null;
        }

        Budget budget = new Budget();
        budget.setName(request.getName());
        budget.setLimitAmount(request.getLimitAmount());
        budget.setSpent(request.getSpent());
        budget.setCategories(categories != null ? new ArrayList<>(categories) : new ArrayList<>());

        return budget;
    }

    public BudgetRequest toRequest(Budget budget) {
        if (budget == null) {
            return null;
        }

        BudgetRequest request = new BudgetRequest();
        request.setName(budget.getName());
        request.setLimitAmount(budget.getLimitAmount());
        request.setSpent(budget.getSpent());

        request.setCategoryIds(
                budget.getCategories() != null ? budget.getCategories().stream().map(Category::getId).toList() : null);

        return request;
    }
}
