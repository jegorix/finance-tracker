package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.BudgetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;

    @Override
    public BudgetResponse findById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id));
        return budgetMapper.toResponse(budget);
    }

    @Override
    public List<BudgetResponse> findAll() {
        return toResponses(budgetRepository.findAll(), false);
    }

    @Override
    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        User user = getUser(request.getUserId());
        List<Category> categories = getCategoriesForUserIfPresent(request.getCategoryIds(), user.getId());

        Budget budget = budgetMapper.fromRequest(request, user, categories);
        budget.setName(request.getName().trim());
        linkBudgetAndCategories(budget, categories);

        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id));
        User user = getUser(request.getUserId());
        List<Category> categories = getCategoriesForUserIfPresent(request.getCategoryIds(), user.getId());

        budget.setName(request.getName().trim());
        budget.setLimitAmount(request.getLimitAmount());
        budget.setPeriodStart(request.getPeriodStart());
        budget.setPeriodEnd(request.getPeriodEnd());
        budget.setUser(user);
        linkBudgetAndCategories(budget, categories);

        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!budgetRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id);
        }
        budgetRepository.deleteById(id);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + userId));
    }

    private List<Category> getCategoriesForUser(List<Long> categoryIds, Long userId) {
        List<Category> categories = categoryRepository.findAllByIdInAndUserId(categoryIds, userId);
        if (categories.size() != categoryIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Some categories not found for user " + userId);
        }
        return categories;
    }

    private List<Category> getCategoriesForUserIfPresent(List<Long> categoryIds, Long userId) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return getCategoriesForUser(categoryIds, userId);
    }

    private void linkBudgetAndCategories(Budget budget, List<Category> categories) {
        for (Category currentCategory : budget.getCategories()) {
            currentCategory.getBudgets().remove(budget);
        }

        budget.getCategories().clear();
        for (Category category : categories) {
            budget.getCategories().add(category);
            if (!category.getBudgets().contains(budget)) {
                category.getBudgets().add(budget);
            }
        }
    }

    private List<BudgetResponse> toResponses(List<Budget> budgets, boolean includeTransactions) {
        return budgets.stream().map(budget -> budgetMapper.toResponse(budget, includeTransactions)).toList();
    }
}
