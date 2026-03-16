package com.finance.tracker.service.impl;

import com.finance.tracker.cache.TransactionSearchIndexInvalidator;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetPatchRequest;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.BudgetService;
import java.time.LocalDate;
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
    private final TransactionSearchIndexInvalidator transactionSearchIndexInvalidator;

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
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
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
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BudgetResponse patch(Long id, BudgetPatchRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id));

        User user = request.getUserId() != null ? getUser(request.getUserId()) : budget.getUser();
        List<Long> targetCategoryIds = request.getCategoryIds() != null
                ? request.getCategoryIds()
                : budget.getCategories().stream().map(Category::getId).toList();
        List<Category> categories = getCategoriesForUserIfPresent(targetCategoryIds, user.getId());

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget name must not be blank");
            }
            budget.setName(name);
        }
        if (request.getLimitAmount() != null) {
            budget.setLimitAmount(request.getLimitAmount());
        }
        if (request.getPeriodStart() != null) {
            budget.setPeriodStart(request.getPeriodStart());
        }
        if (request.getPeriodEnd() != null) {
            budget.setPeriodEnd(request.getPeriodEnd());
        }

        validatePeriodRange(budget.getPeriodStart(), budget.getPeriodEnd());
        budget.setUser(user);
        if (request.getUserId() != null || request.getCategoryIds() != null) {
            linkBudgetAndCategories(budget, categories);
        }

        Budget saved = budgetRepository.save(budget);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!budgetRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found " + id);
        }
        budgetRepository.deleteById(id);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
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

    private void validatePeriodRange(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart != null && periodEnd != null && periodEnd.isBefore(periodStart)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "periodEnd must be greater than or equal to periodStart");
        }
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
