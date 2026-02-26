package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserWithAccountsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
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
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithAccountsAndTransactions(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return toResponses(userRepository.findAllWithAccounts());
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        List<Account> accounts = getAccounts(request.getAccountIds());
        List<Transaction> transactions = getTransactions(request.getTransactionIds());
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
            user.setAccounts(accounts);
        }
        if (request.getTransactionIds() != null) {
            List<Transaction> transactions = getTransactions(request.getTransactionIds());
            user.setTransactions(transactions);
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

    private UserResponse createUserWithAccounts(UserWithAccountsCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);

        for (int i = 0; i < request.getAccounts().size(); i++) {
            Account account = accountMapper.fromRequest(request.getAccounts().get(i));
            account.setUser(savedUser);
            savedUser.getAccounts().add(account);
            accountRepository.save(account);

            if (request.isFailAfterSecondAccount() && i == 1) {
                throw new IllegalStateException("Forced error after saving user and second account");
            }
        }

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResponse createUserWithAccountsNoTx(UserWithAccountsCreateRequest request) {
        return createUserWithAccounts(request);
    }

    @Override
    @Transactional
    public UserResponse createUserWithAccountsTx(UserWithAccountsCreateRequest request) {
        return createUserWithAccounts(request);
    }
}
