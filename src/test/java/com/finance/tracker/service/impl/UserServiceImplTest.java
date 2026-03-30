package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.AccountType;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndBudgetsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.LoggingException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BudgetRepository budgetRepository;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                userRepository,
                accountRepository,
                budgetRepository,
                new UserMapper(),
                new AccountMapper());
    }

    @Test
    void findByIdShouldReturnMappedResponse() {
        User user = user(1L, "john", "john@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = service.findById(1L);

        assertEquals(1L, response.getId());
        assertEquals("john", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));

        assertTrue(exception.getMessage().contains("User not found 1"));
    }

    @Test
    void findAllShouldReturnMappedResponses() {
        when(userRepository.findAll()).thenReturn(List.of(user(1L, "john", "john@example.com")));

        List<UserResponse> responses = service.findAll();

        assertEquals(1, responses.size());
        assertEquals("john", responses.get(0).getUsername());
    }

    @Test
    void createShouldTrimCredentialsAndSaveUser() {
        UserRequest request = new UserRequest("  john  ", "  john@example.com  ");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = service.create(request);

        assertEquals(1L, response.getId());
        assertEquals("john", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        verify(userRepository).existsByUsernameIgnoreCase("john");
        verify(userRepository).existsByEmailIgnoreCase("john@example.com");
    }

    @Test
    void createShouldRejectNullUsername() {
        UserRequest request = new UserRequest(null, "john@example.com");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Username must not be blank"));
    }

    @Test
    void createShouldRejectBlankUsername() {
        UserRequest request = new UserRequest("   ", "john@example.com");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Username must not be blank"));
    }

    @Test
    void createShouldRejectNullEmail() {
        UserRequest request = new UserRequest("john", null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Email must not be blank"));
    }

    @Test
    void createShouldRejectBlankEmail() {
        UserRequest request = new UserRequest("john", "   ");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Email must not be blank"));
    }

    @Test
    void createShouldRejectDuplicateUsername() {
        UserRequest request = new UserRequest("john", "john@example.com");
        when(userRepository.existsByUsernameIgnoreCase("john")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("username"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createShouldRejectDuplicateEmail() {
        UserRequest request = new UserRequest("john", "john@example.com");
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("email"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateShouldReplaceUsernameAndEmail() {
        User user = user(1L, "john", "john@example.com");
        UserUpdateRequest request = new UserUpdateRequest("  jane  ", "  jane@example.com  ");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.update(1L, request);

        assertEquals("jane", response.getUsername());
        assertEquals("jane@example.com", response.getEmail());
        verify(userRepository).existsByUsernameIgnoreCaseAndIdNot("jane", 1L);
        verify(userRepository).existsByEmailIgnoreCaseAndIdNot("jane@example.com", 1L);
    }

    @Test
    void updateShouldKeepEmailWhenNull() {
        User user = user(1L, "john", "john@example.com");
        UserUpdateRequest request = new UserUpdateRequest("  jane  ", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.update(1L, request);

        assertEquals("jane", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("john@example.com", user.getEmail());
        verify(userRepository, never()).existsByEmailIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    void updateShouldRejectDuplicateUsername() {
        User user = user(1L, "john", "john@example.com");
        UserUpdateRequest request = new UserUpdateRequest("jane", "jane@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("jane", 1L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.update(1L, request));

        assertTrue(exception.getMessage().contains("username"));
    }

    @Test
    void updateShouldRejectDuplicateEmail() {
        User user = user(1L, "john", "john@example.com");
        UserUpdateRequest request = new UserUpdateRequest("jane", "jane@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("jane@example.com", 1L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.update(1L, request));

        assertTrue(exception.getMessage().contains("email"));
    }

    @Test
    void updateShouldRejectBlankEmailWhenProvided() {
        User user = user(1L, "john", "john@example.com");
        UserUpdateRequest request = new UserUpdateRequest("jane", "   ");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.update(1L, request));

        assertTrue(exception.getMessage().contains("Email must not be blank"));
    }

    @Test
    void deleteShouldRemoveExistingUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(userRepository.existsById(1L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.delete(1L));

        assertTrue(exception.getMessage().contains("User not found 1"));
    }

    @Test
    void createWithAccountsAndBudgetsNoTxShouldSaveEverything() {
        UserWithAccountsAndBudgetsCreateRequest request = createRequest(false);
        AtomicLong accountIds = new AtomicLong(10L);
        AtomicLong budgetIds = new AtomicLong(20L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(accountIds.getAndIncrement());
            return account;
        });
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(budgetIds.getAndIncrement());
            return budget;
        });

        UserResponse response = service.createWithAccountsAndBudgetsNoTx(request);

        assertEquals(1L, response.getId());
        assertEquals("john", response.getUsername());
        assertEquals(List.of(10L, 11L), response.getAccountIds());
        assertEquals(List.of(20L), response.getBudgetIds());
    }

    @Test
    void createWithAccountsAndBudgetsTxShouldSaveEverything() {
        UserWithAccountsAndBudgetsCreateRequest request = createRequest(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            return account;
        });
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(20L);
            return budget;
        });

        UserResponse response = service.createWithAccountsAndBudgetsTx(request);

        assertEquals(1L, response.getId());
        assertEquals(List.of(10L, 10L), response.getAccountIds());
        assertEquals(List.of(20L), response.getBudgetIds());
    }

    @Test
    void createWithAccountsAndBudgetsTxShouldThrowAfterAccounts() {
        UserWithAccountsAndBudgetsCreateRequest request = createRequest(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoggingException exception = assertThrows(LoggingException.class,
                () -> service.createWithAccountsAndBudgetsTx(request));

        assertTrue(exception.getMessage().contains("Forced error"));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateShouldAllowNullEmail() {
        User user = user(1L, "john", "john@example.com");
        UserUpdateRequest request = new UserUpdateRequest("johnny", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.update(1L, request);

        assertEquals("johnny", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertNull(request.getEmail());
    }

    private static User user(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    private static UserWithAccountsAndBudgetsCreateRequest createRequest(boolean failAfterAccounts) {
        UserWithAccountsAndBudgetsCreateRequest request = new UserWithAccountsAndBudgetsCreateRequest();
        request.setUsername("  john  ");
        request.setEmail("  john@example.com  ");
        request.setAccounts(List.of(
                accountRequest("  Main card  ", AccountType.DEBIT, "100.00", 1L),
                accountRequest("  Savings  ", AccountType.SAVINGS, "200.00", 1L)));
        request.setBudgets(List.of(budgetRequest("  Groceries  ", "300.00", 1L)));
        request.setFailAfterAccounts(failAfterAccounts);
        return request;
    }

    private static AccountRequest accountRequest(String name, AccountType type, String balance, Long userId) {
        AccountRequest request = new AccountRequest();
        request.setName(name);
        request.setType(type);
        request.setBalance(new BigDecimal(balance));
        request.setUserId(userId);
        return request;
    }

    private static BudgetRequest budgetRequest(String name, String limitAmount, Long userId) {
        BudgetRequest request = new BudgetRequest();
        request.setName(name);
        request.setLimitAmount(new BigDecimal(limitAmount));
        request.setPeriodStart(LocalDate.of(2026, 3, 1));
        request.setPeriodEnd(LocalDate.of(2026, 3, 31));
        request.setUserId(userId);
        return request;
    }
}
