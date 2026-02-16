package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
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
    private final TransactionMapper transactionMapper;

    @Override
    public TransactionResponse getTransactionById(final Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found " + id));
        return transactionMapper.toResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getTransactionsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findAll();
        if (startDate == null && endDate == null) {
            return toResponses(transactions);
        }
        return toResponses(transactionRepository.findByDateBetween(startDate, endDate));
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Budget budget = getBudget(request.getBudgetId());
        Transaction transaction = transactionMapper.fromRequest(request, budget);
        Transaction saved = transactionRepository.save(transaction);
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found" + id));
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

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream().map(transactionMapper::toResponse).toList();
    }
}
