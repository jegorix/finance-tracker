package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    private CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(
                categoryRepository,
                budgetRepository,
                userRepository,
                new CategoryMapper());
    }

    @Test
    void findByIdShouldReturnMappedResponse() {
        Category category = category(1L, "Food", user(5L));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = service.findById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Food", response.getName());
        assertEquals(5L, response.getUserId());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));

        assertTrue(exception.getMessage().contains("Category not found 1"));
    }

    @Test
    void findAllShouldReturnMappedResponses() {
        when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "Food", user(5L))));

        List<CategoryResponse> responses = service.findAll();

        assertEquals(1, responses.size());
        assertEquals("Food", responses.get(0).getName());
    }

    @Test
    void createShouldLinkBudgets() {
        User user = user(5L);
        Budget budget = budget(10L, "Groceries", user);
        CategoryRequest request = categoryRequest("  Food  ", 5L, List.of(10L));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(budgetRepository.findAllById(List.of(10L))).thenReturn(List.of(budget));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });

        CategoryResponse response = service.create(request);

        assertEquals(1L, response.getId());
        assertEquals("Food", response.getName());
        assertEquals(List.of(10L), response.getBudgetIds());
        assertTrue(budget.getCategories().stream().anyMatch(category -> category.getId().equals(1L)));
    }

    @Test
    void createShouldAcceptNullBudgets() {
        User user = user(5L);
        CategoryRequest request = categoryRequest("Food", 5L, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.create(request);

        assertEquals(List.of(), response.getBudgetIds());
    }

    @Test
    void createShouldAcceptEmptyBudgets() {
        User user = user(5L);
        CategoryRequest request = categoryRequest("Food", 5L, List.of());
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.create(request);

        assertEquals(List.of(), response.getBudgetIds());
    }

    @Test
    void createShouldRejectMissingUser() {
        CategoryRequest request = categoryRequest("Food", 5L, null);
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("User not found 5"));
    }

    @Test
    void createShouldRejectNullName() {
        User user = user(5L);
        CategoryRequest request = categoryRequest(null, 5L, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Category name must not be blank"));
    }

    @Test
    void createShouldRejectBlankName() {
        User user = user(5L);
        CategoryRequest request = categoryRequest("   ", 5L, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Category name must not be blank"));
    }

    @Test
    void createShouldRejectUnknownBudgetsForUser() {
        User user = user(5L);
        User otherUser = user(6L);
        Budget budget = budget(10L, "Groceries", otherUser);
        CategoryRequest request = categoryRequest("Food", 5L, List.of(10L));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(budgetRepository.findAllById(List.of(10L))).thenReturn(List.of(budget));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("Some budgets not found"));
    }

    @Test
    void createShouldRejectBudgetWithoutOwner() {
        User user = user(5L);
        Budget budget = budget(10L, "Groceries", null);
        CategoryRequest request = categoryRequest("Food", 5L, List.of(10L));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(budgetRepository.findAllById(List.of(10L))).thenReturn(List.of(budget));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("Some budgets not found"));
    }

    @Test
    void createShouldRejectDuplicateName() {
        User user = user(5L);
        CategoryRequest request = categoryRequest("Food", 5L, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameIgnoreCaseAndUserId("Food", 5L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void updateShouldReplaceProvidedValuesAndRelinkBudgets() {
        User currentUser = user(5L);
        User newUser = user(6L);
        Budget oldBudget = budget(10L, "Groceries", currentUser);
        Budget newBudgetWithCategory = budget(11L, "Travel", newUser);
        Budget newBudgetWithoutCategory = budget(12L, "Bills", newUser);
        Category category = category(1L, "Food", currentUser);
        category.getBudgets().add(oldBudget);
        oldBudget.getCategories().add(category);
        newBudgetWithCategory.getCategories().add(category);
        CategoryUpdateRequest request = new CategoryUpdateRequest("  Travel  ", 6L, List.of(11L, 12L));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newUser));
        when(budgetRepository.findAllById(List.of(11L, 12L)))
                .thenReturn(List.of(newBudgetWithCategory, newBudgetWithoutCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.update(1L, request);

        assertEquals("Travel", response.getName());
        assertEquals(6L, response.getUserId());
        assertEquals(List.of(11L, 12L), response.getBudgetIds());
        assertFalse(oldBudget.getCategories().contains(category));
        assertEquals(1, newBudgetWithCategory.getCategories().size());
        assertTrue(newBudgetWithoutCategory.getCategories().contains(category));
    }

    @Test
    void updateShouldRelinkWhenOnlyBudgetIdsAreProvided() {
        User user = user(5L);
        Budget oldBudget = budget(10L, "Groceries", user);
        Budget newBudget = budget(11L, "Travel", user);
        Category category = category(1L, "Food", user);
        category.getBudgets().add(oldBudget);
        oldBudget.getCategories().add(category);
        CategoryUpdateRequest request = new CategoryUpdateRequest(null, null, List.of(11L));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(budgetRepository.findAllById(List.of(11L))).thenReturn(List.of(newBudget));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.update(1L, request);

        assertEquals(List.of(11L), response.getBudgetIds());
        assertFalse(oldBudget.getCategories().contains(category));
        assertTrue(newBudget.getCategories().contains(category));
    }

    @Test
    void updateShouldKeepExistingValuesWhenOptionalFieldsAreMissing() {
        User user = user(5L);
        Budget budget = budget(10L, "Groceries", user);
        Category category = category(1L, "Food", user);
        category.getBudgets().add(budget);
        budget.getCategories().add(category);
        CategoryUpdateRequest request = new CategoryUpdateRequest(null, null, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(budgetRepository.findAllById(List.of(10L))).thenReturn(List.of(budget));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.update(1L, request);

        assertEquals("Food", response.getName());
        assertEquals(5L, response.getUserId());
        assertEquals(List.of(10L), response.getBudgetIds());
    }

    @Test
    void updateShouldRejectDuplicateName() {
        User user = user(5L);
        Category category = category(1L, "Food", user);
        CategoryUpdateRequest request = new CategoryUpdateRequest("Travel", null, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndUserIdAndIdNot("Travel", 5L, 1L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> service.update(1L, request));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void deleteShouldRemoveExistingCategory() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(categoryRepository.existsById(1L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.delete(1L));

        assertTrue(exception.getMessage().contains("Category not found 1"));
        verify(categoryRepository, never()).deleteById(1L);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Budget budget(Long id, String name, User user) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setName(name);
        budget.setLimitAmount(new BigDecimal("200.00"));
        budget.setPeriodStart(LocalDate.of(2026, 3, 1));
        budget.setPeriodEnd(LocalDate.of(2026, 3, 31));
        budget.setUser(user);
        return budget;
    }

    private static Category category(Long id, String name, User user) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setUser(user);
        return category;
    }

    private static CategoryRequest categoryRequest(String name, Long userId, List<Long> budgetIds) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setUserId(userId);
        request.setBudgetIds(budgetIds);
        return request;
    }
}
