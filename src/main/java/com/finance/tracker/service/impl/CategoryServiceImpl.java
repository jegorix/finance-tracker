package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.response.CategoryResponse;
import com.finance.tracker.mapper.CategoryMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> findAll() {
        return toResponses(categoryRepository.findAll());
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        User user = getUser(request.getUserId());
        List<Budget> budgets = getBudgetsForUserIfPresent(request.getBudgetIds(), user.getId());

        Category category = categoryMapper.fromRequest(request, user, List.of());
        category.setName(request.getName().trim());
        linkCategoryAndBudgets(category, budgets);

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found " + id));
        User user = getUser(request.getUserId());
        List<Budget> budgets = getBudgetsForUserIfPresent(request.getBudgetIds(), user.getId());

        category.setName(request.getName().trim());
        category.setUser(user);
        linkCategoryAndBudgets(category, budgets);

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found " + id);
        }
        categoryRepository.deleteById(id);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + userId));
    }

    private List<Budget> getBudgetsForUser(List<Long> budgetIds, Long userId) {
        List<Budget> budgets = budgetRepository.findAllById(budgetIds).stream()
                .filter(budget -> budget.getUser() != null && userId.equals(budget.getUser().getId()))
                .toList();
        if (budgets.size() != budgetIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Some budgets not found for user " + userId);
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
}
