package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndTransactionsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.UserService;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithAccountsAndTransactions(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return toResponses(userRepository.findAllWithAccountsAndTransactions());
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        List<Account> accounts = getAccounts(request.getAccountIds());
        List<Transaction> transactions = getTransactions(request.getTransactionIds());
        ensureAssignableAccounts(accounts, null);
        ensureAssignableTransactions(transactions, null);
        User user = userMapper.fromRequest(request, accounts, transactions);
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found " + id));
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAccountIds() != null) {
            List<Account> accounts = getAccounts(request.getAccountIds());
            ensureAssignableAccounts(accounts, user.getId());
            user.getAccounts().forEach(a -> a.setUser(null));
            user.setAccounts(accounts);
            accounts.forEach(a -> a.setUser(user));
        }
        if (request.getTransactionIds() != null) {
            List<Transaction> transactions = getTransactions(request.getTransactionIds());
            ensureAssignableTransactions(transactions, user.getId());
            user.getTransactions().forEach(t -> t.setUser(null));
            user.setTransactions(transactions);
            transactions.forEach(t -> t.setUser(user));
        }
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found " + id);
        }
        userRepository.deleteById(id);
    }

    private List<Account> getAccounts(List<Long> accountIds) {
        List<Account> accounts = accountRepository.findAllById(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new EntityNotFoundException("Some accounts not found");
        }
        return accounts;
    }

    private List<Transaction> getTransactions(List<Long> transactionIds) {
        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);
        if (transactions.size() != transactionIds.size()) {
            throw new EntityNotFoundException("Some transactions not found");
        }
        return transactions;
    }

    private List<UserResponse> toResponses(List<User> users) {
        return users.stream().map(userMapper::toResponse).toList();
    }

    private void ensureAssignableAccounts(List<Account> accounts, Long currentUserId) {
        for (Account account : accounts) {
            boolean hasOwner = account.getUser() != null;
            boolean belongsToCurrentUser =
                    currentUserId != null && hasOwner && currentUserId.equals(account.getUser().getId());

            if (hasOwner && !belongsToCurrentUser) {
                throw new IllegalStateException("Account " + account.getId() + " already belongs to another user");
            }
        }
    }

    private void ensureAssignableTransactions(List<Transaction> transactions, Long currentUserId) {
        for (Transaction transaction : transactions) {
            boolean hasOwner = transaction.getUser() != null;
            boolean belongsToCurrentUser = currentUserId != null && hasOwner
                    && currentUserId.equals(transaction.getUser().getId());

            if (hasOwner && !belongsToCurrentUser) {
                throw new IllegalStateException(
                        "Transaction " + transaction.getId() + " already belongs to another user");
            }
        }
    }

    private Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found: " + budgetId));
    }

    private UserResponse createUserWithAccountsAndTransactions(
            UserWithAccountsAndTransactionsCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);

        for (AccountRequest accountRequest : request.getAccounts()) {
            Account account = accountMapper.fromRequest(accountRequest);
            account.setUser(savedUser);
            savedUser.getAccounts().add(account);
            accountRepository.save(account);
        }

        int savedTransactions = 0;
        for (TransactionRequest transactionRequest : request.getTransactions()) {
            if (request.isFailAfterTransaction() && savedTransactions == 1) {
                throw new IllegalStateException("Forced error while saving second transaction");
            }

            Budget budget = getBudget(transactionRequest.getBudgetId());
            Transaction transaction = transactionMapper.fromRequest(transactionRequest, budget);
            transaction.setUser(savedUser);
            savedUser.getTransactions().add(transaction);
            transactionRepository.save(transaction);
            savedTransactions++;
        }

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResponse createUserWithAccountsAndTransactionsNoTx(
            UserWithAccountsAndTransactionsCreateRequest request) {
        return createUserWithAccountsAndTransactions(request);
    }

    @Override
    @Transactional
    public UserResponse createUserWithAccountsAndTransactionsTx(
            UserWithAccountsAndTransactionsCreateRequest request) {
        return createUserWithAccountsAndTransactions(request);
    }
}
