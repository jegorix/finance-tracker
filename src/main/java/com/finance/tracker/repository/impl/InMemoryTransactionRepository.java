package com.finance.tracker.repository.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.finance.tracker.domain.Transaction;
import com.finance.tracker.repository.TransactionRepository;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> transactions = new ArrayList<>();

    public InMemoryTransactionRepository() {
        transactions.add(new Transaction(
                1L,
                new BigDecimal("19.90"),
                "Food",
                "Lunch",
                LocalDate.now().minusDays(2)));
        transactions.add(new Transaction(
                2L,
                new BigDecimal("59.00"),
                "Transport",
                "Monthly pass",
                LocalDate.now().minusDays(7)));
        transactions.add(new Transaction(
                3L,
                new BigDecimal("120.00"),
                "Utilities",
                "Electricity",
                LocalDate.now().minusDays(5)));
    }

    @Override
    public Optional<Transaction> getById(long id) {
        return transactions.stream()
                .filter(transaction -> transaction.getId() == id)
                .findFirst();
    }

    @Override
    public List<Transaction> getAll() {
        return Collections.unmodifiableList(transactions);
    }
}
