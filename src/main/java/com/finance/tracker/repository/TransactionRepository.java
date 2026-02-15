package com.finance.tracker.repository;

import com.finance.tracker.domain.Transaction;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Optional<Transaction> getById(long id);

    List<Transaction> getAll();
}