package com.finance.tracker.repository;

import com.finance.tracker.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCategoryNameIgnoreCase(String category);

    @EntityGraph(attributePaths = {"account", "account.owner", "category", "tags"})
    List<Transaction> findWithRelationsByCategoryNameIgnoreCase(String category);

    @EntityGraph(attributePaths = {"account", "account.owner", "category", "tags"})
    List<Transaction> findAllWithRelations();

    @EntityGraph(attributePaths = {"account", "account.owner", "category", "tags"})
    Optional<Transaction> findWithRelationsById(Long id);
}
