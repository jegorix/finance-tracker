package com.finance.tracker.service.impl;

import com.finance.tracker.auth.AuthContext;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.request.CategoryUpdateRequest;
import com.finance.tracker.dto.response.CategoryResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.CategoryMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponse(getCategory(id));
    }

    @Override
    public List<CategoryResponse> findAll() {
        Long currentUserId = currentUserId();
        return toResponses(currentUserId == null
                ? categoryRepository.findAll()
                : categoryRepository.findAllByUserId(currentUserId));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Long currentUserId = currentUserId();
        if (currentUserId != null) {
            ensureCurrentUser(request.getUserId(), currentUserId);
        }
        User user = getUser(currentUserId != null ? currentUserId : request.getUserId());
        List<Budget> budgets = getBudgetsForUserIfPresent(request.getBudgetIds(), user.getId());
        String normalizedName = normalizeName(request.getName());
        ensureUniqueCategoryName(normalizedName, user.getId(), null);

        Category category = categoryMapper.fromRequest(request, user, List.of());
        category.setName(normalizedName);
        linkCategoryAndBudgets(category, budgets);

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        Category category = getCategory(id);
        Long currentUserId = currentUserId();
        Long targetUserId = request.getUserId() != null ? request.getUserId() : category.getUser().getId();
        User user;
        if (currentUserId != null) {
            ensureCurrentUser(targetUserId, currentUserId);
            user = getUser(currentUserId);
        } else {
            user = request.getUserId() != null ? getUser(targetUserId) : category.getUser();
        }
        List<Long> targetBudgetIds = request.getBudgetIds() != null
                ? request.getBudgetIds()
                : category.getBudgets().stream().map(Budget::getId).toList();
        List<Budget> budgets = getBudgetsForUserIfPresent(targetBudgetIds, user.getId());
        String normalizedName = request.getName() != null ? normalizeName(request.getName()) : category.getName();
        ensureUniqueCategoryName(normalizedName, user.getId(), category.getId());

        if (request.getName() != null) {
            category.setName(normalizedName);
        }
        if (request.getUserId() != null) {
            category.setUser(user);
        }
        if (request.getUserId() != null || request.getBudgetIds() != null) {
            linkCategoryAndBudgets(category, budgets);
        }

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long currentUserId = currentUserId();
        boolean exists = currentUserId == null
                ? categoryRepository.existsById(id)
                : categoryRepository.existsByIdAndUserId(id, currentUserId);
        if (!exists) {
            throw new ResourceNotFoundException("Category not found " + id);
        }
        categoryRepository.deleteById(id);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + userId));
    }

    private List<Budget> getBudgetsForUser(List<Long> budgetIds, Long userId) {
        List<Budget> budgets = budgetRepository.findAllById(budgetIds).stream()
                .filter(budget -> budget.getUser() != null && userId.equals(budget.getUser().getId()))
                .toList();
        if (budgets.size() != budgetIds.size()) {
            throw new ResourceNotFoundException("Some budgets not found for user " + userId);
        }
        return budgets;
    }

    private List<Budget> getBudgetsForUserIfPresent(List<Long> budgetIds, Long userId) {
        if (budgetIds == null || budgetIds.isEmpty()) {
            return List.of();
        }
        return getBudgetsForUser(budgetIds, userId);
    }

    private void linkCategoryAndBudgets(Category category, List<Budget> budgets) {
        for (Budget currentBudget : category.getBudgets()) {
            currentBudget.getCategories().remove(category);
        }

        category.getBudgets().clear();
        for (Budget budget : budgets) {
            category.getBudgets().add(budget);
            if (!budget.getCategories().contains(category)) {
                budget.getCategories().add(category);
            }
        }
    }

    private List<CategoryResponse> toResponses(List<Category> categories) {
        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    private Category getCategory(Long id) {
        Long currentUserId = currentUserId();
        return (currentUserId == null
                ? categoryRepository.findById(id)
                : categoryRepository.findByIdAndUserId(id, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found " + id));
    }

    private Long currentUserId() {
        return AuthContext.getCurrentUserId();
    }

    private void ensureCurrentUser(Long requestUserId, Long currentUserId) {
        if (!currentUserId.equals(requestUserId)) {
            throw new ResourceNotFoundException("User not found " + requestUserId);
        }
    }

    private void ensureUniqueCategoryName(String name, Long userId, Long currentCategoryId) {
        boolean exists = currentCategoryId == null
                ? categoryRepository.existsByNameIgnoreCaseAndUserId(name, userId)
                : categoryRepository.existsByNameIgnoreCaseAndUserIdAndIdNot(name, userId, currentCategoryId);
        if (exists) {
            throw new DuplicateResourceException("Category with name '" + name + "' already exists for user " + userId);
        }
    }

    private String normalizeName(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Category name must not be blank");
        }
        return normalized;
    }
}
