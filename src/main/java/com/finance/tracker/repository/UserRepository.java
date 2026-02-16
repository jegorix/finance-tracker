package com.finance.tracker.repository;

import com.finance.tracker.domain.User;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "accounts")
    @Query("SELECT u FROM User u")
    List<User> findAllWithAccounts();

    @EntityGraph(attributePaths = {"accounts", "transactions"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    java.util.Optional<User> findByIdWithAccountsAndTransactions(Long id);
}
