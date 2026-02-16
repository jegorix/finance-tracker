package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.response.CategoryResponse;
import com.finance.tracker.mapper.CategoryMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.service.CategoryService;

import jakarta.persistence.EntityNotFoundException;

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
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return toResponses(categoryRepository.findAll());
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        List<Budget> budgets = getBudgets(request.getBudgetIds());
        Category category = categoryMapper.fromRequest(request, budgets);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found " + id));
        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getBudgetIds() != null) {
            List<Budget> budgets = getBudgets(request.getBudgetIds());
            category.setBudgets(budgets);
        }
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Category not found " + id);
        }
        categoryRepository.deleteById(id);
    }

    private List<Budget> getBudgets(List<Long> budgetIds) {
        List<Budget> budgets = budgetRepository.findAllById(budgetIds);
        if (budgets.size() != budgetIds.size()) {
            throw new EntityNotFoundException("Some budgets not found");
        }
        return budgets;
    }

    private List<CategoryResponse> toResponses(List<Category> categories) {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }
}
