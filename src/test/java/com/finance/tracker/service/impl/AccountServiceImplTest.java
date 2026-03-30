package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finance.tracker.cache.TransactionSearchIndexInvalidator;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.AccountType;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.AccountUpdateRequest;
import com.finance.tracker.dto.request.TransferDemoRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.dto.response.TransferDemoResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.LoggingException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionSearchIndexInvalidator transactionSearchIndexInvalidator;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(
                accountRepository,
                userRepository,
                new AccountMapper(),
                transactionSearchIndexInvalidator);
    }

    @Test
    void findByIdShouldReturnMappedResponse() {
        Account account = account(10L, "Main card", AccountType.DEBIT, "125.50", user(1L));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        AccountResponse response = service.findById(10L);

        assertEquals(10L, response.getId());
        assertEquals("Main card", response.getName());
        assertEquals(AccountType.DEBIT, response.getType());
        assertAmount("125.50", response.getBalance());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(accountRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(10L));

        assertTrue(exception.getMessage().contains("Account not found 10"));
    }

    @Test
    void findAllShouldReturnMappedResponses() {
        when(accountRepository.findAll()).thenReturn(List.of(
                account(1L, "Cash", AccountType.CASH, "10.00", user(1L)),
                account(2L, "Savings", AccountType.SAVINGS, "20.00", user(1L))));

        List<AccountResponse> responses = service.findAll();

        assertEquals(2, responses.size());
        assertEquals("Cash", responses.get(0).getName());
        assertEquals("Savings", responses.get(1).getName());
    }

    @Test
    void createShouldTrimNameAndInvalidateCache() {
        User user = user(5L);
        AccountRequest request = accountRequest("  Main card  ", AccountType.CHECKING, "150.00", 5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(11L);
            return account;
        });

        AccountResponse response = service.create(request);

        assertEquals(11L, response.getId());
        assertEquals("Main card", response.getName());
        verify(accountRepository).existsByNameIgnoreCaseAndUserId("Main card", 5L);
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void createShouldThrowWhenUserMissing() {
        AccountRequest request = accountRequest("Main", AccountType.CHECKING, "150.00", 5L);
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("User not found 5"));
        verifyNoInteractions(transactionSearchIndexInvalidator);
    }

    @Test
    void createShouldRejectNullName() {
        User user = user(5L);
        AccountRequest request = accountRequest(null, AccountType.CHECKING, "150.00", 5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Account name must not be blank"));
    }

    @Test
    void createShouldRejectBlankName() {
        User user = user(5L);
        AccountRequest request = accountRequest("   ", AccountType.CHECKING, "150.00", 5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.create(request));

        assertTrue(exception.getMessage().contains("Account name must not be blank"));
    }

    @Test
    void createShouldRejectDuplicateName() {
        User user = user(5L);
        AccountRequest request = accountRequest("Main", AccountType.CHECKING, "150.00", 5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(accountRepository.existsByNameIgnoreCaseAndUserId("Main", 5L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.create(request));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateShouldChangeProvidedFieldsAndInvalidateCache() {
        User currentUser = user(5L);
        User newUser = user(6L);
        Account account = account(10L, "Old name", AccountType.CHECKING, "100.00", currentUser);
        AccountUpdateRequest request = new AccountUpdateRequest("  New name  ", AccountType.SAVINGS,
                new BigDecimal("210.00"), 6L);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newUser));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service.update(10L, request);

        assertEquals("New name", response.getName());
        assertEquals(AccountType.SAVINGS, response.getType());
        assertAmount("210.00", response.getBalance());
        assertEquals(newUser, account.getUser());
        verify(accountRepository).existsByNameIgnoreCaseAndUserIdAndIdNot("New name", 6L, 10L);
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void updateShouldKeepExistingFieldsWhenOptionalFieldsAreMissing() {
        User currentUser = user(5L);
        Account account = account(10L, "Old name", AccountType.CHECKING, "100.00", currentUser);
        AccountUpdateRequest request = new AccountUpdateRequest(null, null, null, null);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service.update(10L, request);

        assertEquals("Old name", response.getName());
        assertEquals(AccountType.CHECKING, response.getType());
        assertAmount("100.00", response.getBalance());
        assertEquals(currentUser, account.getUser());
        verify(accountRepository).existsByNameIgnoreCaseAndUserIdAndIdNot("Old name", 5L, 10L);
    }

    @Test
    void updateShouldRejectBlankName() {
        Account account = account(10L, "Old name", AccountType.CHECKING, "100.00", user(5L));
        AccountUpdateRequest request = new AccountUpdateRequest("   ", null, null, null);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.update(10L, request));

        assertTrue(exception.getMessage().contains("Account name must not be blank"));
    }

    @Test
    void updateShouldRejectDuplicateName() {
        Account account = account(10L, "Old name", AccountType.CHECKING, "100.00", user(5L));
        AccountUpdateRequest request = new AccountUpdateRequest("Duplicate", null, null, null);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.existsByNameIgnoreCaseAndUserIdAndIdNot("Duplicate", 5L, 10L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> service.update(10L, request));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void transferTxShouldMoveMoneyAndInvalidateCache() {
        Account fromAccount = account(1L, "From", AccountType.CHECKING, "100.00", user(1L));
        Account toAccount = account(2L, "To", AccountType.SAVINGS, "50.00", user(1L));
        TransferDemoRequest request = new TransferDemoRequest(1L, 2L, new BigDecimal("20.00"), false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferDemoResponse response = service.transferTx(request);

        assertEquals(1L, response.getFromAccountId());
        assertAmount("80.00", response.getFromAccountBalance());
        assertEquals(2L, response.getToAccountId());
        assertAmount("70.00", response.getToAccountBalance());
        assertAmount("20.00", response.getTransferredAmount());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void transferNoTxShouldMoveMoneyAndInvalidateCache() {
        Account fromAccount = account(1L, "From", AccountType.CHECKING, "100.00", user(1L));
        Account toAccount = account(2L, "To", AccountType.SAVINGS, "50.00", user(1L));
        TransferDemoRequest request = new TransferDemoRequest(1L, 2L, new BigDecimal("10.00"), false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferDemoResponse response = service.transferNoTx(request);

        assertAmount("90.00", response.getFromAccountBalance());
        assertAmount("60.00", response.getToAccountBalance());
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void transferTxShouldRejectSameAccount() {
        TransferDemoRequest request = new TransferDemoRequest(1L, 1L, new BigDecimal("10.00"), false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.transferTx(request));

        assertTrue(exception.getMessage().contains("two different accounts"));
        verifyNoInteractions(accountRepository, transactionSearchIndexInvalidator);
    }

    @Test
    void transferTxShouldRejectInsufficientFunds() {
        Account fromAccount = account(1L, "From", AccountType.CHECKING, "5.00", user(1L));
        Account toAccount = account(2L, "To", AccountType.SAVINGS, "50.00", user(1L));
        TransferDemoRequest request = new TransferDemoRequest(1L, 2L, new BigDecimal("10.00"), false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.transferTx(request));

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(transactionSearchIndexInvalidator);
    }

    @Test
    void transferTxShouldFailAfterDebit() {
        Account fromAccount = account(1L, "From", AccountType.CHECKING, "100.00", user(1L));
        Account toAccount = account(2L, "To", AccountType.SAVINGS, "50.00", user(1L));
        TransferDemoRequest request = new TransferDemoRequest(1L, 2L, new BigDecimal("10.00"), true);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoggingException exception = assertThrows(LoggingException.class, () -> service.transferTx(request));

        assertTrue(exception.getMessage().contains("Forced error"));
        verify(accountRepository, times(1)).save(any(Account.class));
        verifyNoInteractions(transactionSearchIndexInvalidator);
    }

    @Test
    void deleteShouldRemoveExistingAccountAndInvalidateCache() {
        when(accountRepository.existsById(10L)).thenReturn(true);

        service.delete(10L);

        verify(accountRepository).deleteById(10L);
        verify(transactionSearchIndexInvalidator).invalidateAfterCommitOrNow();
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(accountRepository.existsById(10L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.delete(10L));

        assertInstanceOf(ResourceNotFoundException.class, exception);
        verify(accountRepository, never()).deleteById(10L);
        verifyNoInteractions(transactionSearchIndexInvalidator);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Account account(Long id, String name, AccountType type, String balance, User user) {
        Account account = new Account();
        account.setId(id);
        account.setName(name);
        account.setType(type);
        account.setBalance(new BigDecimal(balance));
        account.setUser(user);
        return account;
    }

    private static AccountRequest accountRequest(String name, AccountType type, String balance, Long userId) {
        AccountRequest request = new AccountRequest();
        request.setName(name);
        request.setType(type);
        request.setBalance(new BigDecimal(balance));
        request.setUserId(userId);
        return request;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
