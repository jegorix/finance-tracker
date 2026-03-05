package com.finance.tracker.mapper;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return toResponse(transaction, true, true, true);
    }

    public TransactionResponse toResponse(
            Transaction transaction,
            boolean includeBudget,
            boolean includeAccount,
            boolean includeUser) {
        if (transaction == null) {
            return null;
        }

        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setOccurredAt(transaction.getOccurredAt());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setType(transaction.getType());
        if (includeBudget) {
            response.setBudgetId(
                    transaction.getBudget() != null ? transaction.getBudget().getId() : null);
            response.setBudgetName(
                    transaction.getBudget() != null ? transaction.getBudget().getName() : null);
        }
        if (includeAccount) {
            response.setAccountId(
                    transaction.getAccount() != null ? transaction.getAccount().getId() : null);
            response.setAccountName(
                    transaction.getAccount() != null ? transaction.getAccount().getName() : null);
        }
        if (includeUser) {
            response.setUserId(
                    transaction.getUser() != null ? transaction.getUser().getId() : null);
            response.setUsername(
                    transaction.getUser() != null ? transaction.getUser().getUsername() : null);
        }

        return response;
    }

    public Transaction fromRequest(TransactionRequest request, Budget budget, Account account, User user) {
        if (request == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setOccurredAt(request.getOccurredAt());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setType(request.getType());
        transaction.setBudget(budget);
        transaction.setAccount(account);
        transaction.setUser(user);

        return transaction;
    }

    public TransactionRequest toRequest(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionRequest request = new TransactionRequest();
        request.setOccurredAt(transaction.getOccurredAt());
        request.setAmount(transaction.getAmount());
        request.setDescription(transaction.getDescription());
        request.setType(transaction.getType());
        request.setBudgetId(
                transaction.getBudget() != null ? transaction.getBudget().getId() : null);
        request.setAccountId(
                transaction.getAccount() != null ? transaction.getAccount().getId() : null);
        request.setUserId(
                transaction.getUser() != null ? transaction.getUser().getId() : null);

        return request;
    }
}
