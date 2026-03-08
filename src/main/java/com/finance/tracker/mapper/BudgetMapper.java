package com.finance.tracker.mapper;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
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
        response.setPeriodStart(budget.getPeriodStart());
        response.setPeriodEnd(budget.getPeriodEnd());
        response.setUserId(budget.getUser() != null ? budget.getUser().getId() : null);

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

    public Budget fromRequest(BudgetRequest request, User user, List<Category> categories) {
        if (request == null) {
            return null;
        }

        Budget budget = new Budget();
        budget.setName(request.getName());
        budget.setLimitAmount(request.getLimitAmount());
        budget.setPeriodStart(request.getPeriodStart());
        budget.setPeriodEnd(request.getPeriodEnd());
        budget.setUser(user);
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
        request.setPeriodStart(budget.getPeriodStart());
        request.setPeriodEnd(budget.getPeriodEnd());
        request.setUserId(budget.getUser() != null ? budget.getUser().getId() : null);

        request.setCategoryIds(
                budget.getCategories() != null ? budget.getCategories().stream().map(Category::getId).toList() : null);

        return request;
    }
}
