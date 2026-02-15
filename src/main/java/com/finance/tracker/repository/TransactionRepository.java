package com.finance.tracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finance.tracker.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
