package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.TransactionService;

import java.time.LocalDate;
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
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));
        return transactionMapper.toResponse(transaction, true, true);
    }

    @Override
    public List<TransactionResponse> getAllTransactions(boolean withEntityGraph) {
        List<Transaction> transactions = withEntityGraph
                ? transactionRepository.findAllTransactionsWithEntityGraph()
                : transactionRepository.findAllTransactions();
        return toResponses(transactions);
    }

    @Override
    public List<TransactionResponse> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both startDate and endDate are required for date range filtering");
        }

        List<Transaction> transactions = transactionRepository.findByDateBetween(startDate, endDate);
        return toResponses(transactions);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Budget budget = getBudget(request.getBudgetId());
        Transaction transaction = transactionMapper.fromRequest(request, budget);
        if (request.getUserId() != null) {
            transaction.setUser(getUser(request.getUserId()));
        }
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found " + id));
        if (request.getDate() != null) {
            transaction.setDate(request.getDate());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        if (request.getBudgetId() != null) {
            Budget budget = getBudget(request.getBudgetId());
            transaction.setBudget(budget);
        }
        if (request.getUserId() != null) {
            transaction.setUser(getUser(request.getUserId()));
        }
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
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

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> transactionMapper.toResponse(transaction, true, true))
                .toList();
    }
}
