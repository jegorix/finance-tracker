package com.finance.tracker.repository;

import com.finance.tracker.domain.Budget;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = { "categories", "transactions" })
    @Query("SELECT DISTINCT b FROM Budget b")
    List<Budget> findAllWithTransactions();

    @EntityGraph(attributePaths = "categories")
    @Query("SELECT DISTINCT b FROM Budget b")
    List<Budget> findAllWithCategories();

    @EntityGraph(attributePaths = { "categories", "transactions" })
    @Query("SELECT b FROM Budget b WHERE b.id = :id")
    Optional<Budget> findByIdWithCategoriesAndTransactions(@Param("id") Long id);
}
