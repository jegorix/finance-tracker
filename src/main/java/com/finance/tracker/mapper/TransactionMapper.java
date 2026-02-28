package com.finance.tracker.mapper;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.entity.Account;
import com.finance.tracker.entity.Category;
import com.finance.tracker.entity.Tag;
import com.finance.tracker.entity.Transaction;
import java.util.Set;
import java.util.stream.Collectors;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setDate(transaction.getDate());

        if (transaction.getAccount() != null) {
            response.setAccountName(transaction.getAccount().getName());
            if (transaction.getAccount().getOwner() != null) {
                response.setOwnerEmail(transaction.getAccount().getOwner().getEmail());
            }
        }

        if (transaction.getCategory() != null) {
            response.setCategoryName(transaction.getCategory().getName());
        }

        response.setTags(transaction.getTags().stream()
            .map(Tag::getName)
            .collect(Collectors.toList()));

        return response;
    }

    public static Transaction toEntity(
        TransactionRequest request,
        Account account,
        Category category,
        Set<Tag> tags
    ) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setTags(tags);
        return transaction;
    }

    public static Transaction updateEntity(
        Transaction existing,
        TransactionRequest request,
        Account account,
        Category category,
        Set<Tag> tags
    ) {
        existing.setAmount(request.getAmount());
        existing.setDate(request.getDate());
        existing.setAccount(account);
        existing.setCategory(category);
        existing.getTags().clear();
        existing.getTags().addAll(tags);
        return existing;
    }
}
