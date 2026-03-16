package com.finance.tracker.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.finance.tracker.domain.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByOccurredAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);
    boolean existsByAccountId(Long accountId);
    boolean existsByBudgetId(Long budgetId);

    @Query("SELECT t FROM Transaction t")
    List<Transaction> findAllTransactions();

    @EntityGraph(attributePaths = { "budget", "account" })
    @Query("SELECT t FROM Transaction t")
    List<Transaction> findAllTransactionsWithEntityGraph();

    @EntityGraph(attributePaths = { "budget", "account" })
    @Query("""
            SELECT t
            FROM Transaction t
            LEFT JOIN t.budget b
            JOIN t.account a
            WHERE COALESCE(LOWER(b.name), '') LIKE LOWER(CONCAT('%', :budgetName, '%'))
              AND LOWER(a.name) LIKE LOWER(CONCAT('%', :accountName, '%'))
              AND t.amount >= :minAmount
              AND t.amount <= :maxAmount
              AND t.occurredAt >= :startDateTime
              AND t.occurredAt <= :endDateTime
            """)
    Page<Transaction> searchByNestedFiltersJpql(
            @Param("budgetName") String budgetName,
            @Param("accountName") String accountName,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);

    @Query(
            value = """
                    SELECT t.*
                    FROM transactions t
                    JOIN accounts a ON a.id = t.account_id
                    LEFT JOIN budgets b ON b.id = t.budget_id
                    WHERE COALESCE(LOWER(b.name), '') LIKE LOWER(CONCAT('%', :budgetName, '%'))
                      AND LOWER(a.name) LIKE LOWER(CONCAT('%', :accountName, '%'))
                      AND t.amount >= :minAmount
                      AND t.amount <= :maxAmount
                      AND t.occurred_at >= :startDateTime
                      AND t.occurred_at <= :endDateTime
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM transactions t
                    JOIN accounts a ON a.id = t.account_id
                    LEFT JOIN budgets b ON b.id = t.budget_id
                    WHERE COALESCE(LOWER(b.name), '') LIKE LOWER(CONCAT('%', :budgetName, '%'))
                      AND LOWER(a.name) LIKE LOWER(CONCAT('%', :accountName, '%'))
                      AND t.amount >= :minAmount
                      AND t.amount <= :maxAmount
                      AND t.occurred_at >= :startDateTime
                      AND t.occurred_at <= :endDateTime
                    """,
            nativeQuery = true)
    Page<Transaction> searchByNestedFiltersNative(
            @Param("budgetName") String budgetName,
            @Param("accountName") String accountName,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);
}
