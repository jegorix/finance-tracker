package com.finance.tracker.mapper;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.entity.Transaction;

public class TransactionMapper {

    public static TransactionResponse toResponse(Transaction transaction) {

        final TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setCategory(transaction.getCategory());
        response.setDate(transaction.getDate());
        return response;
    }
    
    public static Transaction toEntity(Transaction request) {
        final Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDate(request.getDate());
        return transaction;
    }

}