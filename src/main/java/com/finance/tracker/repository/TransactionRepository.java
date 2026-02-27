package com.finance.tracker.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.finance.tracker.domain.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t")
    List<Transaction> findAllTransactions();

    @EntityGraph(attributePaths = { "budget", "user" })
    @Query("SELECT t FROM Transaction t")
    List<Transaction> findAllTransactionsWithEntityGraph();
}
