package com.financeflow.repository;
import com.financeflow.entity.Transaction;
import com.financeflow.enums.TransactionType;
import com.financeflow.repository.projection.CategoryAmountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.user.id = :userId and t.transactionType = :transactionType")
    BigDecimal getTotalAmountByUserIdAndTransactionType(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType
    );

    @Query("select t.category.name as categoryName, sum(t.amount) as totalAmount " +
            "from Transaction t where t.user.id = :userId and t.transactionType = :transactionType " +
            "group by t.category.id, t.category.name order by sum(t.amount) desc")
    List<CategoryAmountProjection> getCategoryTotalsByUserIdAndTransactionType(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType
    );

    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);
    List<Transaction> findTop5ByUserIdOrderByTransactionDateDesc(Long userId);
    long countByUserId(Long userId);
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
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
