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

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public TransactionResponse getTransactionById(Long id, boolean withBudget, boolean withUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found " + id));
        return transactionMapper.toResponse(transaction, withBudget, withUser);
    }

    @Override
    public List<TransactionResponse> getTransactionsByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            boolean withBudget,
            boolean withUser) {
        if (startDate == null && endDate == null) {
            List<Transaction> transactions = getAllTransactions(withBudget, withUser);
            return toResponses(transactions, withBudget, withUser);
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both startDate and endDate are required for date range filtering");
        }

        List<Transaction> transactions = transactionRepository.findByDateBetween(startDate, endDate);
        return toResponses(transactions, withBudget, withUser);
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
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found " + id));
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
            throw new EntityNotFoundException("Transaction not found " + id);
        }
        transactionRepository.deleteById(id);
    }

    private Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found: " + budgetId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private List<Transaction> getAllTransactions(boolean withBudget, boolean withUser) {
        if (withBudget && !withUser) {
            return transactionRepository.findAllWithBudget();
        }
        if (withUser && !withBudget) {
            return transactionRepository.findAllWithUser();
        }
        return transactionRepository.findAll();
    }

    private List<TransactionResponse> toResponses(
            List<Transaction> transactions,
            boolean withBudget,
            boolean withUser) {
        return transactions.stream()
                .map(transaction -> transactionMapper.toResponse(transaction, withBudget, withUser))
                .toList();
    }
}
