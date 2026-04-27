package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finance.tracker.cache.TransactionSearchIndex;
import com.finance.tracker.cache.TransactionSearchIndexInvalidator;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionSearchCriteria;
import com.finance.tracker.dto.request.TransactionSearchQueryMode;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.dto.response.TransactionSearchResult;
import com.finance.tracker.dto.response.TransactionSearchSource;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionSearchIndexInvalidator transactionSearchIndexInvalidator;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(
                transactionRepository,
                budgetRepository,
                accountRepository,
                new TransactionMapper(),
                new TransactionSearchIndex(),
                transactionSearchIndexInvalidator);
    }

    @Test
    void findByIdShouldReturnMappedResponse() {
        Transaction transaction = transaction(
                5L,
                account(1L, "Main card", user(1L)),
                budget(2L, "Groceries", user(1L)));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));

        TransactionResponse response = service.findById(5L);

        assertEquals(5L, response.getId());
        assertEquals("Lunch", response.getDescription());
        assertEquals(1L, response.getAccountId());
        assertEquals(2L, response.getBudgetId());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(transactionRepository.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(5L));

        assertTrue(exception.getMessage().contains("Transaction not found 5"));
    }

    @Test
    void findAllShouldUseEntityGraphWhenRequested() {
        when(transactionRepository.findAllTransactionsWithEntityGraph())
                .thenReturn(List.of(transaction(
                        5L,
                        account(1L, "Main card", user(1L)),
                        budget(2L, "Groceries", user(1L)))));

        List<TransactionResponse> responses = service.findAll(true);

        assertEquals(1, responses.size());
        verify(transactionRepository).findAllTransactionsWithEntityGraph();
    }

    @Test
    void findAllShouldUseSimpleQueryWhenEntityGraphDisabled() {
        when(transactionRepository.findAllTransactions())
                .thenReturn(List.of(transaction(5L, account(1L, "Main card", user(1L)), null)));

        List<TransactionResponse> responses = service.findAll(false);

        assertEquals(1, responses.size());
        assertNull(responses.get(0).getBudgetId());
        verify(transactionRepository).findAllTransactions();
    }

    @Test
    void findByDateRangeShouldReturnMatches() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 31, 23, 59);
        when(transactionRepository.findByOccurredAtBetween(start, end))
                .thenReturn(List.of(transaction(5L, account(1L, "Main card", user(1L)), null)));

        List<TransactionResponse> responses = service.findByDateRange(start, end);

        assertEquals(1, responses.size());
    }

    @Test
    void findByDateRangeShouldRejectMissingStart() {
        LocalDateTime end = LocalDateTime.of(2026, 3, 31, 23, 59);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.findByDateRange(null, end));

        assertTrue(exception.getMessage().contains("Both startDateTime and endDateTime are required"));
    }

    @Test
    void findByDateRangeShouldRejectMissingEnd() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 0, 0);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.findByDateRange(start, null));

        assertTrue(exception.getMessage().contains("Both startDateTime and endDateTime are required"));
    }

    @Test
    void findByDateRangeShouldRejectInvalidRange() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 31, 23, 59);
        LocalDateTime end = LocalDateTime.of(2026, 3, 1, 0, 0);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.findByDateRange(start, end));

        assertTrue(exception.getMessage().contains("endDateTime must be greater than or equal to startDateTime"));
    }

    @Test
    void searchShouldHandleNullCriteria() {
        Page<Transaction> page = transactionPage(
                PageRequest.of(0, 5),
                transaction(5L, account(1L, "Main card", user(1L)), null));
        when(transactionRepository.searchByNestedFiltersJpql(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class))).thenReturn(page);

        TransactionSearchResult result = service.search(null, PageRequest.of(0, 5));

        assertEquals(TransactionSearchSource.DATABASE, result.getSource());
        verify(transactionRepository).searchByNestedFiltersJpql(
                "",
                "",
                new BigDecimal("-999999999999.99"),
                new BigDecimal("999999999999.99"),
                LocalDateTime.of(1, 1, 1, 0, 0),
                LocalDateTime.of(9999, 12, 31, 23, 59, 59),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "occurredAt")));
    }

    @Test
    void searchShouldUseCacheOnRepeatedJpqlRequest() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 31, 23, 59);
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                null,
                "  Food  ",
                "  Main card  ",
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                start,
                end);
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Transaction> page = transactionPage(pageable, transaction(5L, account(1L, "Main card", user(1L)), null));
        when(transactionRepository.searchByNestedFiltersJpql(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class))).thenReturn(page);

        TransactionSearchResult first = service.search(criteria, pageable);
        TransactionSearchResult second = service.search(criteria, pageable);

        assertEquals(TransactionSearchSource.DATABASE, first.getSource());
        assertEquals(TransactionSearchSource.CACHE, second.getSource());
        verify(transactionRepository, times(1)).searchByNestedFiltersJpql(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class));
    }

    @Test
    void searchShouldUseNativeQueryAndNormalizeBlankFilters() {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                TransactionSearchQueryMode.NATIVE,
                "   ",
                " ",
                new BigDecimal("5.00"),
                null,
                LocalDateTime.of(2026, 3, 10, 0, 0),
                null);
        Page<Transaction> page = transactionPage(
                PageRequest.of(0, 3),
                transaction(5L, account(1L, "Main card", user(1L)), null));
        when(transactionRepository.searchByNestedFiltersNative(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class))).thenReturn(page);

        service.search(criteria, PageRequest.of(0, 3));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).searchByNestedFiltersNative(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                pageableCaptor.capture());
        Pageable repositoryPageable = pageableCaptor.getValue();
        assertEquals(Sort.Direction.DESC, repositoryPageable.getSort().getOrderFor("occurred_at").getDirection());
        verify(transactionRepository).searchByNestedFiltersNative(
                "",
                "",
                new BigDecimal("5.00"),
                new BigDecimal("999999999999.99"),
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(9999, 12, 31, 23, 59, 59),
                repositoryPageable);
    }

    @Test
    void searchShouldRejectWhenMaxAmountLessThanMinAmount() {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                TransactionSearchQueryMode.JPQL,
                null,
                null,
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                null,
                null);
        PageRequest pageable = PageRequest.of(0, 5);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.search(criteria, pageable));

        assertTrue(exception.getMessage().contains("maxAmount must be greater than or equal to minAmount"));
    }

    @Test
    void searchShouldRejectWhenEndBeforeStart() {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                TransactionSearchQueryMode.JPQL,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 2, 0, 0),
                LocalDateTime.of(2026, 3, 1, 0, 0));
        PageRequest pageable = PageRequest.of(0, 5);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.search(criteria, pageable));

        assertTrue(exception.getMessage().contains("endDateTime must be greater than or equal to startDateTime"));
    }

    @Test
    void searchShouldNormalizeAllSupportedJpqlSortProperties() {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                TransactionSearchQueryMode.JPQL,
                null,
                null,
                null,
                null,
                null,
                null);
        Sort sort = Sort.by(List.of(
                Sort.Order.asc("id"),
                Sort.Order.desc("occurredAt"),
                Sort.Order.asc("occurred_at"),
                Sort.Order.asc("amount"),
                Sort.Order.desc("description").ignoreCase(),
                Sort.Order.asc("type")));
        PageRequest pageable = PageRequest.of(0, 5, sort);
        when(transactionRepository.searchByNestedFiltersJpql(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class))).thenReturn(transactionPage(pageable));

        service.search(criteria, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).searchByNestedFiltersJpql(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                pageableCaptor.capture());
        List<Sort.Order> orders = pageableCaptor.getValue().getSort().stream().toList();
        assertEquals(List.of("id", "occurredAt", "occurredAt", "amount", "description", "type"),
                orders.stream().map(Sort.Order::getProperty).toList());
        assertTrue(orders.get(4).isIgnoreCase());
    }

    @Test
    void searchShouldRejectUnsupportedJpqlSortProperty() {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                TransactionSearchQueryMode.JPQL,
                null,
                null,
                null,
                null,
                null,
                null);
        PageRequest pageable = PageRequest.of(0, 5, Sort.by("unsupported"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.search(criteria, pageable));

        assertTrue(exception.getMessage().contains("Unsupported sort property for JPQL query"));
    }

    @Test
    void searchShouldNormalizeAllSupportedNativeSortProperties() {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                TransactionSearchQueryMode.NATIVE,
                null,
                null,
                null,
                null,
                null,
                null);
        Sort sort = Sort.by(List.of(
                Sort.Order.asc("id"),
                Sort.Order.desc("occurredAt"),
                Sort.Order.asc("occurred_at"),
                Sort.Order.asc("amount"),
                Sort.Order.desc("description").ignoreCase(),
                Sort.Order.asc("type"),
                Sort.Order.asc("budgetId"),
                Sort.Order.asc("budget_id"),
                Sort.Order.asc("accountId"),
                Sort.Order.asc("account_id")));
        PageRequest pageable = PageRequest.of(0, 5, sort);
        when(transactionRepository.searchByNestedFiltersNative(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class))).thenReturn(transactionPage(pageable));

        service.search(criteria, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).searchByNestedFiltersNative(
                anyString(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                pageableCaptor.capture());
        List<Sort.Order> orders = pageableCaptor.getValue().getSort().stream().toList();
        assertEquals(List.of(
                        "id",
                        "occurred_at",
                        "occurred_at",
                        "amount",
                        "description",
                        "type",
                        "budget_id",
                        "budget_id",
                        "account_id",
                        "account_id"),
                orders.stream().map(Sort.Order::getProperty).toList());
        assertTrue(orders.get(4).isIgnoreCase());
    }

    @Test
    void searchShouldRejectUnsupportedNativeSortProperty() {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria(
                TransactionSearchQueryMode.NATIVE,
                null,
                null,
                null,
                null,
                null,
                null);
        PageRequest pageable = PageRequest.of(0, 5, Sort.by("unsupported"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.search(criteria, pageable));

        assertTrue(exception.getMessage().contains("Unsupported sort property for native query"));
    }

    @Test
    void createShouldCreateTransaction() {
        User user = user(1L);
        Account account = account(1L, "Main card", user);
        Budget budget = budget(2L, "Groceries", user);
        TransactionRequest request = request("  Lunch  ", TransactionType.EXPENSE, "20.00", 1L, 2L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(5L);
            return transaction;
        });

        TransactionResponse response = service.create(request);

        assertEquals(5L, response.getId());
        assertEquals("Lunch", response.getDescription());
        assertEquals(2L, response.getBudgetId());
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void createShouldRejectNullDescription() {
        Account account = account(1L, "Main card", user(1L));
        TransactionRequest request = request(null, TransactionType.EXPENSE, "20.00", 1L, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Description must not be blank"));
    }

    @Test
    void createShouldRejectBlankDescription() {
        Account account = account(1L, "Main card", user(1L));
        TransactionRequest request = request("   ", TransactionType.EXPENSE, "20.00", 1L, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Description must not be blank"));
    }

    @Test
    void createShouldFailWhenAccountMissing() {
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("Account not found: 1"));
    }

    @Test
    void createShouldFailWhenBudgetMissing() {
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, 2L);
        when(budgetRepository.findById(2L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("Budget not found: 2"));
    }

    @Test
    void createShouldFailWhenAccountHasNoOwner() {
        Account account = account(1L, "Main card", null);
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Account must be owned by a user"));
    }

    @Test
    void createShouldFailWhenBudgetHasNoOwner() {
        User user = user(1L);
        Account account = account(1L, "Main card", user);
        Budget budget = budget(2L, "Groceries", null);
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, 2L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Budget must be owned by a user"));
    }

    @Test
    void createShouldFailWhenBudgetOwnerAndAccountOwnerDoNotMatch() {
        Account account = account(1L, "Main card", user(1L));
        Budget budget = budget(2L, "Groceries", user(2L));
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, 2L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Budget owner and account owner must match"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createBulkTxShouldCreateAllTransactions() {
        User user = user(1L);
        Account account = account(1L, "Main card", user);
        Budget budget = budget(2L, "Groceries", user);
        TransactionRequest salary = request("Salary", TransactionType.INCOME, "50.00", 1L, null);
        TransactionRequest lunch = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, 2L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget));
        AtomicLong ids = new AtomicLong(10L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(ids.getAndIncrement());
            return transaction;
        });

        List<TransactionResponse> responses = service.createBulkTx(List.of(salary, lunch));

        assertEquals(2, responses.size());
        assertEquals(10L, responses.get(0).getId());
        assertEquals(11L, responses.get(1).getId());
        verify(transactionSearchIndexInvalidator, times(2)).invalidateAfterCommitOrNow();
    }

    @Test
    void createBulkNoTxShouldCreateAllTransactions() {
        User user = user(1L);
        Account account = account(1L, "Main card", user);
        TransactionRequest salary = request("Salary", TransactionType.INCOME, "50.00", 1L, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(10L);
            return transaction;
        });

        List<TransactionResponse> responses = service.createBulkNoTx(List.of(salary));

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).getId());
    }

    @Test
    void createBulkTxShouldRejectEmptyRequest() {
        List<TransactionRequest> emptyRequests = List.of();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createBulkTx(emptyRequests));

        assertTrue(exception.getMessage().contains("at least one item"));
        verifyNoInteractions(
                transactionRepository,
                budgetRepository,
                accountRepository,
                transactionSearchIndexInvalidator);
    }

    @Test
    void createBulkNoTxShouldRejectNullRequest() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createBulkNoTx(null));

        assertTrue(exception.getMessage().contains("at least one item"));
    }

    @Test
    void updateShouldReplaceAllFieldsAndInvalidate() {
        User user = user(1L);
        Transaction transaction = transaction(5L, account(1L, "Main card", user), null);
        Budget budget = budget(2L, "Groceries", user);
        TransactionRequest request = request("  New lunch  ", TransactionType.TRANSFER, "40.00", 1L, 2L);
        request.setOccurredAt(LocalDateTime.of(2026, 3, 18, 15, 0));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(transaction.getAccount()));
        when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.update(5L, request);

        assertEquals("New lunch", response.getDescription());
        assertEquals(TransactionType.TRANSFER, response.getType());
        assertEquals(2L, response.getBudgetId());
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void updateShouldAllowNullBudget() {
        User user = user(1L);
        Transaction transaction = transaction(5L, account(1L, "Main card", user), budget(2L, "Groceries", user));
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, null);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(transaction.getAccount()));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.update(5L, request);

        assertNull(response.getBudgetId());
    }

    @Test
    void updateShouldRejectBlankDescription() {
        User user = user(1L);
        Transaction transaction = transaction(5L, account(1L, "Main card", user), null);
        TransactionRequest request = request("   ", TransactionType.EXPENSE, "20.00", 1L, null);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(transaction.getAccount()));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.update(5L, request));

        assertTrue(exception.getMessage().contains("Description must not be blank"));
    }

    @Test
    void updateShouldThrowWhenMissing() {
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, null);
        when(transactionRepository.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(5L, request));

        assertTrue(exception.getMessage().contains("Transaction not found 5"));
    }

    @Test
    void patchShouldKeepCurrentValuesWhenOptionalFieldsAreMissing() {
        User user = user(1L);
        Account account = account(1L, "Main card", user);
        Budget budget = budget(2L, "Groceries", user);
        Transaction transaction = transaction(5L, account, budget);
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAmount(new BigDecimal("10.00"));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.patch(5L, request);

        assertEquals("Lunch", response.getDescription());
        assertEquals(TransactionType.EXPENSE, response.getType());
        assertAmount("10.00", response.getAmount());
        assertEquals(1L, response.getAccountId());
        assertEquals(2L, response.getBudgetId());
        verify(accountRepository, never()).findById(any());
        verify(budgetRepository, never()).findById(any());
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void patchShouldUpdateAllProvidedFields() {
        User currentUser = user(1L);
        User newUser = user(2L);
        Account newAccount = account(3L, "Travel card", newUser);
        Budget newBudget = budget(4L, "Travel", newUser);
        Transaction transaction = transaction(
                5L,
                account(1L, "Main card", currentUser),
                budget(2L, "Groceries", currentUser));
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setOccurredAt(LocalDateTime.of(2026, 4, 1, 9, 0));
        request.setAmount(new BigDecimal("99.00"));
        request.setDescription("  Taxi  ");
        request.setType(TransactionType.TRANSFER);
        request.setBudgetId(4L);
        request.setAccountId(3L);
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(newAccount));
        when(budgetRepository.findById(4L)).thenReturn(Optional.of(newBudget));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.patch(5L, request);

        assertEquals("Taxi", response.getDescription());
        assertEquals(TransactionType.TRANSFER, response.getType());
        assertEquals(3L, response.getAccountId());
        assertEquals(4L, response.getBudgetId());
    }

    @Test
    void patchShouldAllowNullExistingBudget() {
        User user = user(1L);
        Transaction transaction = transaction(5L, account(1L, "Main card", user), null);
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setDescription("  Updated  ");
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.patch(5L, request);

        assertEquals("Updated", response.getDescription());
        assertNull(response.getBudgetId());
    }

    @Test
    void deleteShouldRemoveExistingTransactionAndInvalidateCache() {
        when(transactionRepository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(transactionRepository).deleteById(5L);
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(transactionRepository.existsById(5L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));

        assertTrue(exception.getMessage().contains("Transaction not found 5"));
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Account account(Long id, String name, User user) {
        Account account = new Account();
        account.setId(id);
        account.setName(name);
        account.setUser(user);
        return account;
    }

    private static Budget budget(Long id, String name, User user) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setName(name);
        budget.setUser(user);
        return budget;
    }

    private static TransactionRequest request(
            String description,
            TransactionType type,
            String amount,
            Long accountId,
            Long budgetId) {
        TransactionRequest request = new TransactionRequest();
        request.setOccurredAt(LocalDateTime.of(2026, 3, 17, 10, 0));
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);
        request.setType(type);
        request.setAccountId(accountId);
        request.setBudgetId(budgetId);
        return request;
    }

    private static Transaction transaction(Long id, Account account, Budget budget) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setOccurredAt(LocalDateTime.of(2026, 3, 17, 12, 0));
        transaction.setAmount(new BigDecimal("30.00"));
        transaction.setDescription("Lunch");
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAccount(account);
        transaction.setBudget(budget);
        return transaction;
    }

    private static Page<Transaction> transactionPage(Pageable pageable, Transaction... transactions) {
        return new PageImpl<>(List.of(transactions), pageable, transactions.length);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
