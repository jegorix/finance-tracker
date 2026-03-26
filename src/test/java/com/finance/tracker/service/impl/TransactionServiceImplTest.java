package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ConflictException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionSearchIndex transactionSearchIndex;

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
                transactionSearchIndex,
                transactionSearchIndexInvalidator);
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
        assertEquals(1L, responses.get(0).getAccountId());
        assertEquals("Main card", responses.get(0).getAccountName());
        assertEquals(2L, responses.get(1).getBudgetId());
        assertEquals("Groceries", responses.get(1).getBudgetName());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(transactionSearchIndexInvalidator, times(2)).invalidateAfterCommitOrNow();
    }

    @Test
    void createBulkTxShouldRejectEmptyRequest() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createBulkTx(List.of()));

        assertTrue(exception.getMessage().contains("at least one item"));
        verifyNoInteractions(transactionRepository, budgetRepository, accountRepository, transactionSearchIndexInvalidator);
    }

    @Test
    void createShouldFailWhenBudgetOwnerAndAccountOwnerDoNotMatch() {
        Account account = account(1L, "Main card", user(1L));
        Budget budget = budget(2L, "Groceries", user(2L));
        TransactionRequest request = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, 2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget));

        assertThrows(ConflictException.class, () -> service.create(request));

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transactionSearchIndexInvalidator, never()).invalidateAfterCommitOrNow();
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

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
