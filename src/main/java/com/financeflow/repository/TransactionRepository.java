package com.financeflow.repository;
import com.financeflow.entity.Transaction;
import com.financeflow.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByAccountId(Long accountId);
    List<Transaction> findByCategoryId(Long categoryId);
    List<Transaction> findByTransactionType(TransactionType type);
    List<Transaction> findByTransactionDateBetween(
                LocalDateTime start,
                LocalDateTime end
    );
    List<Transaction> findByAmountGreaterThan(BigDecimal amount);

    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    List<Transaction> findTop10ByOrderByTransactionDateDesc();
}
