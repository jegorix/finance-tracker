package com.finance.tracker.mapper;

import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setDate(transaction.getDate());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setBudgetId(
                transaction.getBudget() != null ? transaction.getBudget().getId() : null);
        response.setUserId(
                transaction.getUser() != null ? transaction.getUser().getId() : null);

        return response;
    }

    public Transaction fromRequest(TransactionRequest request, Budget budget) {
        if (request == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setDate(request.getDate());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setBudget(budget);

        return transaction;
    }

    public TransactionRequest toRequest(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionRequest request = new TransactionRequest();
        request.setDate(transaction.getDate());
        request.setAmount(transaction.getAmount());
        request.setDescription(transaction.getDescription());
        request.setBudgetId(
                transaction.getBudget() != null ? transaction.getBudget().getId() : null);
        request.setUserId(
                transaction.getUser() != null ? transaction.getUser().getId() : null);

        return request;
    }
}
