package com.finance.tracker.repository;

import com.finance.tracker.domain.Budget;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = "categories")
    @Query("SELECT b FROM Budget b")
    List<Budget> findAllWithCategories();

    @Query("SELECT DISTINCT b FROM Budget b LEFT JOIN FETCH b.transactions")
    List<Budget> findAllWithTransactions();

    @EntityGraph(attributePaths = { "categories", "transactions" })
    @Query("SELECT b FROM Budget b WHERE b.id = :id")
    java.util.Optional<Budget> findByIdWithCategoriesAndTransactions(Long id);
}
