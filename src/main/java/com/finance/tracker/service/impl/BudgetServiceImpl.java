package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.service.BudgetService;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;

    @Override
    public BudgetResponse getBudgetById(Long id) {
        Budget budget = budgetRepository.findByIdWithCategoriesAndTransactions(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found " + id));
        return budgetMapper.toResponse(budget);
    }

    @Override
    public List<BudgetResponse> getAllBudgets() {
        return toResponses(budgetRepository.findAllWithCategories());
    }

    @Override
    public List<BudgetResponse> getAllBudgetsWithTransactions() {
        return toResponses(budgetRepository.findAllWithTransactions());
    }

    @Override
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        List<Category> categories = getCategories(request.getCategoryIds());
        Budget budget = budgetMapper.fromRequest(request, categories);
        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found " + id));
        if (request.getName() != null) {
            budget.setName(request.getName());
        }
        if (request.getLimitAmount() != null) {
            budget.setLimitAmount(request.getLimitAmount());
        }
        if (request.getSpent() != null) {
            budget.setSpent(request.getSpent());
        }
        if (request.getCategoryIds() != null) {
            List<Category> categories = getCategories(request.getCategoryIds());
            budget.setCategories(categories);
        }
        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        if (!budgetRepository.existsById(id)) {
            throw new EntityNotFoundException("Budget not found " + id);
        }
        budgetRepository.deleteById(id);
    }

    private List<Category> getCategories(List<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new EntityNotFoundException("Some categories not found");
        }
        return categories;
    }

    private List<BudgetResponse> toResponses(List<Budget> budgets) {
        return budgets.stream().map(budgetMapper::toResponse).toList();
    }
}
