package com.financeflow.service;

import com.financeflow.dto.response.dashboard.CategoryAmountResponse;
import com.financeflow.dto.response.dashboard.DashboardResponse;
import com.financeflow.entity.User;
import com.financeflow.enums.TransactionType;
import com.financeflow.mapper.TransactionMapper;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import com.financeflow.repository.projection.CategoryAmountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        return DashboardResponse.builder()
                .totalBalance(accountRepository.getTotalBalanceByUserId(userId))
                .totalIncome(transactionRepository.getTotalAmountByUserIdAndTransactionType(
                        userId, TransactionType.INCOME))
                .totalExpense(transactionRepository.getTotalAmountByUserIdAndTransactionType(
                        userId, TransactionType.EXPENSE))
                .transactionCount(transactionRepository.countByUserId(userId))
                .incomeByCategory(toCategoryAmountResponses(
                        transactionRepository.getCategoryTotalsByUserIdAndTransactionType(
                                userId, TransactionType.INCOME)))
                .expenseByCategory(toCategoryAmountResponses(
                        transactionRepository.getCategoryTotalsByUserIdAndTransactionType(
                                userId, TransactionType.EXPENSE)))
                .recentTransactions(transactionRepository.findTop5ByUserIdOrderByTransactionDateDesc(userId)
                        .stream()
                        .map(transactionMapper::toResponse)
                        .toList())
                .build();
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
