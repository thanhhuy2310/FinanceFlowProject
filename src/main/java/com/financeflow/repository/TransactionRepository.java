package com.financeflow.repository;
import com.financeflow.entity.Transaction;
import com.financeflow.enums.TransactionType;
import com.financeflow.repository.projection.CategoryAmountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Total amount per transaction type.
    // The service layer picks the variant that matches the requested date range
    // (all time, from, until, between) so no null date parameters reach the database.

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.user.id = :userId and t.transactionType = :transactionType")
    BigDecimal getTotalAmountByUserIdAndTransactionType(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType
    );

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.user.id = :userId and t.transactionType = :transactionType " +
            "and t.transactionDate >= :start")
    BigDecimal getTotalAmountByUserIdAndTransactionTypeFrom(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("start") LocalDateTime start
    );

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.user.id = :userId and t.transactionType = :transactionType " +
            "and t.transactionDate < :end")
    BigDecimal getTotalAmountByUserIdAndTransactionTypeUntil(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("end") LocalDateTime end
    );

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.user.id = :userId and t.transactionType = :transactionType " +
            "and t.transactionDate >= :start and t.transactionDate < :end")
    BigDecimal getTotalAmountByUserIdAndTransactionTypeBetween(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Category totals per transaction type

    @Query("select t.category.name as categoryName, sum(t.amount) as totalAmount " +
            "from Transaction t where t.user.id = :userId and t.transactionType = :transactionType " +
            "group by t.category.id, t.category.name order by sum(t.amount) desc")
    List<CategoryAmountProjection> getCategoryTotalsByUserIdAndTransactionType(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType
    );

    @Query("select t.category.name as categoryName, sum(t.amount) as totalAmount " +
            "from Transaction t where t.user.id = :userId and t.transactionType = :transactionType " +
            "and t.transactionDate >= :start " +
            "group by t.category.id, t.category.name order by sum(t.amount) desc")
    List<CategoryAmountProjection> getCategoryTotalsByUserIdAndTransactionTypeFrom(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("start") LocalDateTime start
    );

    @Query("select t.category.name as categoryName, sum(t.amount) as totalAmount " +
            "from Transaction t where t.user.id = :userId and t.transactionType = :transactionType " +
            "and t.transactionDate < :end " +
            "group by t.category.id, t.category.name order by sum(t.amount) desc")
    List<CategoryAmountProjection> getCategoryTotalsByUserIdAndTransactionTypeUntil(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("end") LocalDateTime end
    );

    @Query("select t.category.name as categoryName, sum(t.amount) as totalAmount " +
            "from Transaction t where t.user.id = :userId and t.transactionType = :transactionType " +
            "and t.transactionDate >= :start and t.transactionDate < :end " +
            "group by t.category.id, t.category.name order by sum(t.amount) desc")
    List<CategoryAmountProjection> getCategoryTotalsByUserIdAndTransactionTypeBetween(
            @Param("userId") Long userId,
            @Param("transactionType") TransactionType transactionType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Transaction count

    long countByUserId(Long userId);

    @Query("select count(t) from Transaction t where t.user.id = :userId " +
            "and t.transactionDate >= :start")
    long countByUserIdFrom(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start
    );

    @Query("select count(t) from Transaction t where t.user.id = :userId " +
            "and t.transactionDate < :end")
    long countByUserIdUntil(
            @Param("userId") Long userId,
            @Param("end") LocalDateTime end
    );

    @Query("select count(t) from Transaction t where t.user.id = :userId " +
            "and t.transactionDate >= :start and t.transactionDate < :end")
    long countByUserIdBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Recent transactions

    List<Transaction> findTop5ByUserIdOrderByTransactionDateDesc(Long userId);

    @Query("select t from Transaction t where t.user.id = :userId " +
            "and t.transactionDate >= :start " +
            "order by t.transactionDate desc")
    List<Transaction> findTop5ByUserIdFrom(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            Pageable pageable
    );

    @Query("select t from Transaction t where t.user.id = :userId " +
            "and t.transactionDate < :end " +
            "order by t.transactionDate desc")
    List<Transaction> findTop5ByUserIdUntil(
            @Param("userId") Long userId,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("select t from Transaction t where t.user.id = :userId " +
            "and t.transactionDate >= :start and t.transactionDate < :end " +
            "order by t.transactionDate desc")
    List<Transaction> findTop5ByUserIdBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // Full history ordered by date

    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    @Query("select t from Transaction t where t.user.id = :userId " +
            "and t.transactionDate >= :start " +
            "order by t.transactionDate desc")
    List<Transaction> findByUserIdFromOrderByTransactionDateDesc(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start
    );

    @Query("select t from Transaction t where t.user.id = :userId " +
            "and t.transactionDate < :end " +
            "order by t.transactionDate desc")
    List<Transaction> findByUserIdUntilOrderByTransactionDateDesc(
            @Param("userId") Long userId,
            @Param("end") LocalDateTime end
    );

    @Query("select t from Transaction t where t.user.id = :userId " +
            "and t.transactionDate >= :start and t.transactionDate < :end " +
            "order by t.transactionDate desc")
    List<Transaction> findByUserIdBetweenOrderByTransactionDateDesc(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("select t.reference from Transaction t " +
            "where t.user.id = :userId and t.reference is not null")
    List<String> findReferenceByUserId(@Param("userId") Long userId);

    List<Transaction> findByAccountId(Long accountId);
}
