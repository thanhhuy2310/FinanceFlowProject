package com.financeflow.service;

import com.financeflow.dto.response.dashboard.CategoryAmountResponse;
import com.financeflow.dto.response.dashboard.DashboardResponse;
import com.financeflow.dto.response.transaction.TransactionResponse;
import com.financeflow.entity.User;
import com.financeflow.enums.TransactionType;
import com.financeflow.mapper.TransactionMapper;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import com.financeflow.repository.projection.CategoryAmountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_TRANSACTIONS_LIMIT = 5;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    /** All-time dashboard summary. */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        return getDashboard(null, null);
    }

    /**
     * Dashboard summary restricted to the given date range. A null start or end
     * leaves that bound open, so both bounds omitted equals all time.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDateTime start, LocalDateTime end) {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        return DashboardResponse.builder()
                .totalBalance(accountRepository.getTotalBalanceByUserId(userId))
                .totalIncome(totalAmount(userId, TransactionType.INCOME, start, end))
                .totalExpense(totalAmount(userId, TransactionType.EXPENSE, start, end))
                .transactionCount(transactionCount(userId, start, end))
                .incomeByCategory(toCategoryAmountResponses(
                        categoryTotals(userId, TransactionType.INCOME, start, end)))
                .expenseByCategory(toCategoryAmountResponses(
                        categoryTotals(userId, TransactionType.EXPENSE, start, end)))
                .recentTransactions(recentTransactions(userId, start, end))
                .build();
    }

    private BigDecimal totalAmount(
            Long userId, TransactionType type, LocalDateTime start, LocalDateTime end) {
        return inRange(start, end,
                () -> transactionRepository.getTotalAmountByUserIdAndTransactionType(userId, type),
                from -> transactionRepository.getTotalAmountByUserIdAndTransactionTypeFrom(userId, type, from),
                until -> transactionRepository.getTotalAmountByUserIdAndTransactionTypeUntil(userId, type, until),
                (from, until) -> transactionRepository
                        .getTotalAmountByUserIdAndTransactionTypeBetween(userId, type, from, until));
    }

    private long transactionCount(Long userId, LocalDateTime start, LocalDateTime end) {
        return inRange(start, end,
                () -> transactionRepository.countByUserId(userId),
                from -> transactionRepository.countByUserIdFrom(userId, from),
                until -> transactionRepository.countByUserIdUntil(userId, until),
                (from, until) -> transactionRepository.countByUserIdBetween(userId, from, until));
    }

    private List<CategoryAmountProjection> categoryTotals(
            Long userId, TransactionType type, LocalDateTime start, LocalDateTime end) {
        return inRange(start, end,
                () -> transactionRepository.getCategoryTotalsByUserIdAndTransactionType(userId, type),
                from -> transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeFrom(userId, type, from),
                until -> transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeUntil(userId, type, until),
                (from, until) -> transactionRepository
                        .getCategoryTotalsByUserIdAndTransactionTypeBetween(userId, type, from, until));
    }

    private List<TransactionResponse> recentTransactions(
            Long userId, LocalDateTime start, LocalDateTime end) {
        return inRange(start, end,
                () -> transactionRepository.findTop5ByUserIdOrderByTransactionDateDesc(userId),
                from -> transactionRepository.findTop5ByUserIdFrom(
                        userId, from, PageRequest.of(0, RECENT_TRANSACTIONS_LIMIT)),
                until -> transactionRepository.findTop5ByUserIdUntil(
                        userId, until, PageRequest.of(0, RECENT_TRANSACTIONS_LIMIT)),
                (from, until) -> transactionRepository.findTop5ByUserIdBetween(
                        userId, from, until, PageRequest.of(0, RECENT_TRANSACTIONS_LIMIT)))
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Dispatches to the repository variant matching the requested range so that
     * null date parameters never reach the database (PostgreSQL cannot infer
     * the type of a null bind parameter in "(:param is null)" expressions).
     */
    private <T> T inRange(
            LocalDateTime start,
            LocalDateTime end,
            Supplier<T> allTime,
            Function<LocalDateTime, T> from,
            Function<LocalDateTime, T> until,
            BiFunction<LocalDateTime, LocalDateTime, T> between) {
        if (start != null && end != null) {
            return between.apply(start, end);
        }
        if (start != null) {
            return from.apply(start);
        }
        if (end != null) {
            return until.apply(end);
        }
        return allTime.get();
    }

    private List<CategoryAmountResponse> toCategoryAmountResponses(
            List<CategoryAmountProjection> categoryTotals
    ) {
        return categoryTotals.stream()
                .map(categoryTotal -> CategoryAmountResponse.builder()
                        .categoryName(categoryTotal.getCategoryName())
                        .totalAmount(categoryTotal.getTotalAmount())
                        .build())
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
