package com.finance.tracker.repository;

import com.finance.tracker.domain.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = { "accounts", "transactions" })
    @Query("SELECT DISTINCT u FROM User u")
    List<User> findAllWithAccountsAndTransactions();

    @EntityGraph(attributePaths = { "accounts", "transactions" })
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithAccountsAndTransactions(@Param("id") Long id);
}
