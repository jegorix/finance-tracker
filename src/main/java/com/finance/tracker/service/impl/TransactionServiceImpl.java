package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.TransactionService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));
        return transactionMapper.toResponse(transaction, true, true, true);
    }

    @Override
    public List<TransactionResponse> findAll(boolean withEntityGraph) {
        List<Transaction> transactions = withEntityGraph
                ? transactionRepository.findAllTransactionsWithEntityGraph()
                : transactionRepository.findAllTransactions();
        return toResponses(transactions);
    }

    @Override
    public List<TransactionResponse> findByDateRange(LocalDateTime startDateTime,
            LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both startDateTime and endDateTime are required for date range filtering");
        }

        if (endDateTime.isBefore(startDateTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "endDateTime must be greater than or equal to startDateTime");
        }

        List<Transaction> transactions = transactionRepository.findByOccurredAtBetween(startDateTime, endDateTime);
        return toResponses(transactions);
    }

    @Override
    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        User user = getUser(request.getUserId());
        Budget budget = getBudget(request.getBudgetId());
        Account account = getAccount(request.getAccountId());

        ensureSameOwner(user, budget, account);

        Transaction transaction = transactionMapper.fromRequest(request, budget, account, user);
        transaction.setDescription(request.getDescription().trim());
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));

        Budget budget = getBudget(request.getBudgetId());
        Account account = getAccount(request.getAccountId());
        User user = getUser(request.getUserId());

        ensureSameOwner(user, budget, account);

        transaction.setOccurredAt(request.getOccurredAt());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription().trim());
        transaction.setType(request.getType());
        transaction.setBudget(budget);
        transaction.setAccount(account);
        transaction.setUser(user);

        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id);
        }
        transactionRepository.deleteById(id);
    }

    private Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found: " + budgetId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId));
    }

    private void ensureSameOwner(User user, Budget budget, Account account) {
        if (budget.getUser() == null || account.getUser() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget and account must be owned by a user");
        }

        Long userId = user.getId();
        if (!userId.equals(budget.getUser().getId()) || !userId.equals(account.getUser().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transaction user, account owner and budget owner must match");
        }
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> transactionMapper.toResponse(transaction, true, true, true))
                .toList();
    }
}
