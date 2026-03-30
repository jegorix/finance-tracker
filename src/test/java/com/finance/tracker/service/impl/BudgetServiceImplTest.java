package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionSearchIndexInvalidator transactionSearchIndexInvalidator;

    private BudgetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BudgetServiceImpl(
                budgetRepository,
                categoryRepository,
                userRepository,
                new BudgetMapper(),
                transactionSearchIndexInvalidator);
    }

    @Test
    void findByIdShouldReturnMappedResponse() {
        Budget budget = budget(10L, "Groceries", user(1L));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        BudgetResponse response = service.findById(10L);

        assertEquals(10L, response.getId());
        assertEquals("Groceries", response.getName());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(10L));

        assertTrue(exception.getMessage().contains("Budget not found 10"));
    }

    @Test
    void findAllShouldReturnMappedResponsesWithoutTransactions() {
        when(budgetRepository.findAll()).thenReturn(List.of(budget(1L, "Groceries", user(1L))));

        List<BudgetResponse> responses = service.findAll();

        assertEquals(1, responses.size());
        assertNull(responses.get(0).getTransactionIds());
    }

    @Test
    void createShouldLinkCategoriesAndInvalidateCache() {
        User user = user(1L);
        Category food = category(11L, "Food", user);
        Category home = category(12L, "Home", user);
        BudgetRequest request = budgetRequest("  Groceries  ", "500.00", 1L, List.of(11L, 12L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findAllByIdInAndUserId(List.of(11L, 12L), 1L)).thenReturn(List.of(food, home));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(20L);
            return budget;
        });

        BudgetResponse response = service.create(request);

        assertEquals(20L, response.getId());
        assertEquals("Groceries", response.getName());
        assertEquals(List.of(11L, 12L), response.getCategoryIds());
        verify(budgetRepository).existsByNameIgnoreCaseAndUserId("Groceries", 1L);
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
        assertTrue(food.getBudgets().stream().anyMatch(budget -> budget.getId().equals(20L)));
        assertTrue(home.getBudgets().stream().anyMatch(budget -> budget.getId().equals(20L)));
    }

    @Test
    void createShouldAcceptNullCategoryIds() {
        User user = user(1L);
        BudgetRequest request = budgetRequest("Groceries", "500.00", 1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = service.create(request);

        assertEquals(List.of(), response.getCategoryIds());
    }

    @Test
    void createShouldAcceptEmptyCategoryIds() {
        User user = user(1L);
        BudgetRequest request = budgetRequest("Groceries", "500.00", 1L, List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = service.create(request);

        assertEquals(List.of(), response.getCategoryIds());
    }

    @Test
    void createShouldRejectMissingUser() {
        BudgetRequest request = budgetRequest("Groceries", "500.00", 1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("User not found 1"));
    }

    @Test
    void createShouldRejectUnknownCategoryForUser() {
        User user = user(1L);
        BudgetRequest request = budgetRequest("Groceries", "500.00", 1L, List.of(11L, 12L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findAllByIdInAndUserId(List.of(11L, 12L), 1L))
                .thenReturn(List.of(category(11L, "Food", user)));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("Some categories not found"));
    }

    @Test
    void createShouldRejectNullName() {
        User user = user(1L);
        BudgetRequest request = budgetRequest(null, "500.00", 1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Budget name must not be blank"));
    }

    @Test
    void createShouldRejectBlankName() {
        User user = user(1L);
        BudgetRequest request = budgetRequest("   ", "500.00", 1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Budget name must not be blank"));
    }

    @Test
    void createShouldRejectDuplicateName() {
        User user = user(1L);
        BudgetRequest request = budgetRequest("Groceries", "500.00", 1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(budgetRepository.existsByNameIgnoreCaseAndUserId("Groceries", 1L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void updateShouldReplaceFieldsRelinkCategoriesAndInvalidateCache() {
        User currentUser = user(1L);
        User newUser = user(2L);
        Category oldCategory = category(50L, "Old", currentUser);
        Category newCategoryWithBudget = category(51L, "New A", newUser);
        Category newCategoryWithoutBudget = category(52L, "New B", newUser);
        Budget budget = budget(10L, "Old budget", currentUser);
        budget.getCategories().add(oldCategory);
        oldCategory.getBudgets().add(budget);
        newCategoryWithBudget.getBudgets().add(budget);
        BudgetRequest request = budgetRequest("  New budget  ", "700.00", 2L, List.of(51L, 52L));
        request.setPeriodStart(LocalDate.of(2026, 4, 1));
        request.setPeriodEnd(LocalDate.of(2026, 4, 30));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
        when(categoryRepository.findAllByIdInAndUserId(List.of(51L, 52L), 2L))
                .thenReturn(List.of(newCategoryWithBudget, newCategoryWithoutBudget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = service.update(10L, request);

        assertEquals("New budget", response.getName());
        assertEquals(2L, response.getUserId());
        assertEquals(List.of(51L, 52L), response.getCategoryIds());
        assertFalse(oldCategory.getBudgets().contains(budget));
        assertEquals(1, newCategoryWithBudget.getBudgets().size());
        assertTrue(newCategoryWithoutBudget.getBudgets().contains(budget));
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void patchShouldKeepExistingValuesWhenOptionalFieldsMissing() {
        User user = user(1L);
        Category category = category(11L, "Food", user);
        Budget budget = budget(10L, "Groceries", user);
        budget.getCategories().add(category);
        category.getBudgets().add(budget);
        BudgetUpdateRequest request = new BudgetUpdateRequest(null, null, null, null, null, null);
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));
        when(categoryRepository.findAllByIdInAndUserId(List.of(11L), 1L)).thenReturn(List.of(category));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = service.patch(10L, request);

        assertEquals("Groceries", response.getName());
        assertEquals(1L, response.getUserId());
        assertEquals(List.of(11L), response.getCategoryIds());
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void patchShouldUpdateProvidedFieldsAndRelinkCategories() {
        User currentUser = user(1L);
        User newUser = user(2L);
        Category oldCategory = category(11L, "Food", currentUser);
        Category newCategory = category(12L, "Travel", newUser);
        Budget budget = budget(10L, "Groceries", currentUser);
        budget.getCategories().add(oldCategory);
        oldCategory.getBudgets().add(budget);
        BudgetUpdateRequest request = new BudgetUpdateRequest(
                "  Travel  ",
                new BigDecimal("900.00"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                2L,
                List.of(12L));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
        when(categoryRepository.findAllByIdInAndUserId(List.of(12L), 2L)).thenReturn(List.of(newCategory));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = service.patch(10L, request);

        assertEquals("Travel", response.getName());
        assertEquals(List.of(12L), response.getCategoryIds());
        assertFalse(oldCategory.getBudgets().contains(budget));
        assertTrue(newCategory.getBudgets().contains(budget));
    }

    @Test
    void patchShouldRelinkWhenOnlyCategoryIdsAreProvided() {
        User user = user(1L);
        Category oldCategory = category(11L, "Food", user);
        Category newCategory = category(12L, "Travel", user);
        Budget budget = budget(10L, "Groceries", user);
        budget.setPeriodStart(null);
        budget.getCategories().add(oldCategory);
        oldCategory.getBudgets().add(budget);
        BudgetUpdateRequest request = new BudgetUpdateRequest(null, null, null, null, null, List.of(12L));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));
        when(categoryRepository.findAllByIdInAndUserId(List.of(12L), 1L)).thenReturn(List.of(newCategory));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = service.patch(10L, request);

        assertEquals(List.of(12L), response.getCategoryIds());
        assertFalse(oldCategory.getBudgets().contains(budget));
        assertTrue(newCategory.getBudgets().contains(budget));
    }

    @Test
    void patchShouldAllowMissingPeriodEnd() {
        User user = user(1L);
        Budget budget = budget(10L, "Groceries", user);
        budget.setPeriodEnd(null);
        BudgetUpdateRequest request = new BudgetUpdateRequest(null, null, null, null, null, null);
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = service.patch(10L, request);

        assertEquals("Groceries", response.getName());
        assertEquals(List.of(), response.getCategoryIds());
    }

    @Test
    void patchShouldRejectInvalidPeriodRange() {
        User user = user(1L);
        Budget budget = budget(10L, "Groceries", user);
        BudgetUpdateRequest request = new BudgetUpdateRequest(
                null,
                null,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1),
                null,
                null);
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.patch(10L, request));

        assertTrue(exception.getMessage().contains("periodEnd must be greater than or equal to periodStart"));
    }

    @Test
    void patchShouldRejectDuplicateName() {
        User user = user(1L);
        Budget budget = budget(10L, "Groceries", user);
        BudgetUpdateRequest request = new BudgetUpdateRequest("Travel", null, null, null, null, null);
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));
        when(budgetRepository.existsByNameIgnoreCaseAndUserIdAndIdNot("Travel", 1L, 10L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> service.patch(10L, request));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void deleteShouldRemoveExistingBudgetAndInvalidateCache() {
        when(budgetRepository.existsById(10L)).thenReturn(true);

        service.delete(10L);

        verify(budgetRepository).deleteById(10L);
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(budgetRepository.existsById(10L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.delete(10L));

        assertTrue(exception.getMessage().contains("Budget not found 10"));
        verify(budgetRepository, never()).deleteById(10L);
        verifyNoInteractions(transactionSearchIndexInvalidator);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Category category(Long id, String name, User user) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setUser(user);
        return category;
    }

    private static Budget budget(Long id, String name, User user) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setName(name);
        budget.setLimitAmount(new BigDecimal("100.00"));
        budget.setPeriodStart(LocalDate.of(2026, 3, 1));
        budget.setPeriodEnd(LocalDate.of(2026, 3, 31));
        budget.setUser(user);
        return budget;
    }

    private static BudgetRequest budgetRequest(String name, String limitAmount, Long userId, List<Long> categoryIds) {
        BudgetRequest request = new BudgetRequest();
        request.setName(name);
        request.setLimitAmount(new BigDecimal(limitAmount));
        request.setPeriodStart(LocalDate.of(2026, 3, 1));
        request.setPeriodEnd(LocalDate.of(2026, 3, 31));
        request.setUserId(userId);
        request.setCategoryIds(categoryIds);
        return request;
    }
}
