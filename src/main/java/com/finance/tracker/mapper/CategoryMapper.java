package com.finance.tracker.mapper;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.response.CategoryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }

        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());

        response.setBudgetIds(
                category.getBudgets() != null ? category.getBudgets().stream().map(Budget::getId).toList() : null);

        return response;
    }

    public Category fromRequest(CategoryRequest request, List<Budget> budgets) {
        if (request == null) {
            return null;
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setBudgets(budgets);

        return category;
    }

    public CategoryRequest toRequest(Category category) {
        if (category == null) {
            return null;
        }

        CategoryRequest request = new CategoryRequest();
        request.setName(category.getName());

        request.setBudgetIds(
                category.getBudgets() != null ? category.getBudgets().stream().map(Budget::getId).toList() : null);

        return request;
    }
}
