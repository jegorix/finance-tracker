package com.finance.tracker.service.impl;

import com.finance.tracker.auth.AuthContext;
import com.finance.tracker.cache.TransactionSearchIndexInvalidator;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.BudgetUpdateRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.BudgetMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.BudgetService;
import java.time.LocalDate;
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
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;
    private final TransactionSearchIndexInvalidator transactionSearchIndexInvalidator;

    @Override
    public BudgetResponse findById(Long id) {
        return budgetMapper.toResponse(getBudget(id));
    }

    @Override
    public List<BudgetResponse> findAll() {
        Long currentUserId = currentUserId();
        return toResponses(
                currentUserId == null
                        ? budgetRepository.findAll()
                        : budgetRepository.findAllByUserId(currentUserId),
                false);
    }

    @Override
    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        Long currentUserId = currentUserId();
        if (currentUserId != null) {
            ensureCurrentUser(request.getUserId(), currentUserId);
        }
        User user = getUser(currentUserId != null ? currentUserId : request.getUserId());
        List<Category> categories = getCategoriesForUserIfPresent(request.getCategoryIds(), user.getId());
        String normalizedName = normalizeName(request.getName());
        ensureUniqueBudgetName(normalizedName, user.getId(), null);

        Budget budget = budgetMapper.fromRequest(request, user, categories);
        budget.setName(normalizedName);
        linkBudgetAndCategories(budget, categories);

        Budget saved = budgetRepository.save(budget);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request) {
        Budget budget = getBudget(id);
        Long currentUserId = currentUserId();
        User user;
        if (currentUserId != null) {
            ensureCurrentUser(request.getUserId(), currentUserId);
            user = getUser(currentUserId);
        } else {
            user = request.getUserId() != null ? getUser(request.getUserId()) : budget.getUser();
        }
        List<Category> categories = getCategoriesForUserIfPresent(request.getCategoryIds(), user.getId());
        String normalizedName = normalizeName(request.getName());
        ensureUniqueBudgetName(normalizedName, user.getId(), budget.getId());

        budget.setName(normalizedName);
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
    public BudgetResponse patch(Long id, BudgetUpdateRequest request) {
        Budget budget = getBudget(id);

        Long currentUserId = currentUserId();
        Long targetUserId = request.getUserId() != null ? request.getUserId() : budget.getUser().getId();
        User user;
        if (currentUserId != null) {
            ensureCurrentUser(targetUserId, currentUserId);
            user = getUser(currentUserId);
        } else {
            user = request.getUserId() != null ? getUser(targetUserId) : budget.getUser();
        }
        List<Long> targetCategoryIds = request.getCategoryIds() != null
                ? request.getCategoryIds()
                : budget.getCategories().stream().map(Category::getId).toList();
        List<Category> categories = getCategoriesForUserIfPresent(targetCategoryIds, user.getId());
        String targetName = request.getName() != null ? normalizeName(request.getName()) : budget.getName();
        ensureUniqueBudgetName(targetName, user.getId(), budget.getId());

        if (request.getName() != null) {
            budget.setName(targetName);
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
        Long currentUserId = currentUserId();
        boolean exists = currentUserId == null
                ? budgetRepository.existsById(id)
                : budgetRepository.existsByIdAndUserId(id, currentUserId);
        if (!exists) {
            throw new ResourceNotFoundException("Budget not found " + id);
        }
        budgetRepository.deleteById(id);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + userId));
    }

    private List<Category> getCategoriesForUser(List<Long> categoryIds, Long userId) {
        List<Category> categories = categoryRepository.findAllByIdInAndUserId(categoryIds, userId);
        if (categories.size() != categoryIds.size()) {
            throw new ResourceNotFoundException("Some categories not found for user " + userId);
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
            throw new BadRequestException("periodEnd must be greater than or equal to periodStart");
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

    private Budget getBudget(Long id) {
        Long currentUserId = currentUserId();
        return (currentUserId == null
                ? budgetRepository.findById(id)
                : budgetRepository.findByIdAndUserId(id, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found " + id));
    }

    private Long currentUserId() {
        return AuthContext.getCurrentUserId();
    }

    private void ensureCurrentUser(Long requestUserId, Long currentUserId) {
        if (!currentUserId.equals(requestUserId)) {
            throw new ResourceNotFoundException("User not found " + requestUserId);
        }
    }

    private void ensureUniqueBudgetName(String name, Long userId, Long currentBudgetId) {
        boolean exists = currentBudgetId == null
                ? budgetRepository.existsByNameIgnoreCaseAndUserId(name, userId)
                : budgetRepository.existsByNameIgnoreCaseAndUserIdAndIdNot(name, userId, currentBudgetId);
        if (exists) {
            throw new DuplicateResourceException("Budget with name '" + name + "' already exists for user " + userId);
        }
    }

    private String normalizeName(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Budget name must not be blank");
        }
        return normalized;
    }
}
