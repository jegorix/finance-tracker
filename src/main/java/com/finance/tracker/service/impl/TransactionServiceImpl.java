package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.dto.request.TransactionPatchRequest;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
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
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));
        return transactionMapper.toResponse(transaction, true, true);
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
        Budget budget = getBudget(request.getBudgetId());
        Account account = getAccount(request.getAccountId());

        ensureSameOwner(budget, account);

        Transaction transaction = transactionMapper.fromRequest(request, budget, account);
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

        ensureSameOwner(budget, account);

        transaction.setOccurredAt(request.getOccurredAt());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription().trim());
        transaction.setType(request.getType());
        transaction.setBudget(budget);
        transaction.setAccount(account);

        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse patch(Long id, TransactionPatchRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));

        Budget budget = request.getBudgetId() != null
                ? getBudget(request.getBudgetId())
                : transaction.getBudget();
        Account account = request.getAccountId() != null
                ? getAccount(request.getAccountId())
                : transaction.getAccount();

        ensureSameOwner(budget, account);

        if (request.getOccurredAt() != null) {
            transaction.setOccurredAt(request.getOccurredAt());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            if (description.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must not be blank");
            }
            transaction.setDescription(description);
        }
        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        if (request.getBudgetId() != null) {
            transaction.setBudget(budget);
        }
        if (request.getAccountId() != null) {
            transaction.setAccount(account);
        }

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

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + accountId));
    }

    private void ensureSameOwner(Budget budget, Account account) {
        if (budget.getUser() == null || account.getUser() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget and account must be owned by a user");
        }

        if (!budget.getUser().getId().equals(account.getUser().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget owner and account owner must match");
        }
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> transactionMapper.toResponse(transaction, true, true))
                .toList();
    }
}
