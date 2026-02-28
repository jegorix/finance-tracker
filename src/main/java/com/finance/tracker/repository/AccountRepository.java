package com.finance.tracker.repository;

import com.finance.tracker.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByNameIgnoreCase(String name);
}
