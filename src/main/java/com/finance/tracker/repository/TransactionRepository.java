package com.finance.tracker.repository;
import com.finance.tracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{

    List<Transaction> findByCategory(String category);
} 


// package com.finance.tracker.repository;

// import com.finance.tracker.entity.Transaction;
// import org.springframework.data.jpa.repository.JpaRepository;
// import java.util.List;

// public interface TransactionRepository
//         extends JpaRepository<Transaction, Long> {

//     List<Transaction> findByCategory(String category);
// }