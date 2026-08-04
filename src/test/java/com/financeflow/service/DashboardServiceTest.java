package com.financeflow.service;

import com.financeflow.dto.response.dashboard.CategoryAmountResponse;
import com.financeflow.dto.response.dashboard.DashboardResponse;
import com.financeflow.entity.Account;
import com.financeflow.entity.Category;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.enums.CategoryType;
import com.financeflow.enums.TransactionType;
import com.financeflow.mapper.TransactionMapper;
import com.financeflow.mapper.TransactionMapperImpl;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import com.financeflow.repository.projection.CategoryAmountProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String USER_EMAIL = "user@financeflow.com";
    private static final Long USER_ID = 1L;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @Spy
    private TransactionMapper transactionMapper = new TransactionMapperImpl();

    @InjectMocks
    private DashboardService dashboardService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email(USER_EMAIL).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_EMAIL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CategoryAmountProjection projection(String name, BigDecimal total) {
        CategoryAmountProjection projection = org.mockito.Mockito.mock(CategoryAmountProjection.class);
        when(projection.getCategoryName()).thenReturn(name);
        when(projection.getTotalAmount()).thenReturn(total);
        return projection;
    }

    private Transaction transaction(Account account, Category category, String description) {
        return Transaction.builder()
                .id(1L)
                .user(user)
                .account(account)
                .category(category)
                .amount(new BigDecimal("150000"))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.of(2026, 8, 1, 10, 0))
                .description(description)
                .build();
    }

    @Test
    void getDashboard_calculatesAllTotals() {
        Account account = Account.builder()
                .id(1L)
                .accountName("Techcombank Checking")
                .balance(new BigDecimal("1000000"))
                .build();
        Category food = Category.builder()
                .id(1L)
                .name("Food & Drink")
                .type(CategoryType.EXPENSE)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.getTotalBalanceByUserId(USER_ID)).thenReturn(new BigDecimal("1000000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionType(
                USER_ID, TransactionType.INCOME)).thenReturn(new BigDecimal("2500000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionType(
                USER_ID, TransactionType.EXPENSE)).thenReturn(new BigDecimal("800000"));
        when(transactionRepository.countByUserId(USER_ID)).thenReturn(5L);
        CategoryAmountProjection incomeProjection = projection("Salary", new BigDecimal("2500000"));
        CategoryAmountProjection expenseProjection = projection("Food & Drink", new BigDecimal("800000"));
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionType(
                USER_ID, TransactionType.INCOME)).thenReturn(List.of(incomeProjection));
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionType(
                USER_ID, TransactionType.EXPENSE)).thenReturn(List.of(expenseProjection));
        when(transactionRepository.findTop5ByUserIdOrderByTransactionDateDesc(USER_ID))
                .thenReturn(List.of(transaction(account, food, "Coffee")));

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.getTotalBalance()).isEqualByComparingTo("1000000");
        assertThat(response.getTotalIncome()).isEqualByComparingTo("2500000");
        assertThat(response.getTotalExpense()).isEqualByComparingTo("800000");
        assertThat(response.getTransactionCount()).isEqualTo(5L);
        assertThat(response.getIncomeByCategory())
                .extracting(CategoryAmountResponse::getCategoryName)
                .containsExactly("Salary");
        assertThat(response.getExpenseByCategory())
                .extracting(CategoryAmountResponse::getTotalAmount)
                .containsExactly(new BigDecimal("800000"));
        assertThat(response.getRecentTransactions()).hasSize(1);
        assertThat(response.getRecentTransactions().get(0).getDescription()).isEqualTo("Coffee");
        assertThat(response.getRecentTransactions().get(0).getAccountName()).isEqualTo("Techcombank Checking");
    }

    @Test
    void getDashboard_withDateRange_filtersTransactionsByRange() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 2, 0, 0);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.getTotalBalanceByUserId(USER_ID)).thenReturn(new BigDecimal("1000000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionTypeBetween(
                USER_ID, TransactionType.INCOME, start, end)).thenReturn(new BigDecimal("500000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionTypeBetween(
                USER_ID, TransactionType.EXPENSE, start, end)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByUserIdBetween(USER_ID, start, end)).thenReturn(2L);
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeBetween(
                USER_ID, TransactionType.INCOME, start, end)).thenReturn(List.of());
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeBetween(
                USER_ID, TransactionType.EXPENSE, start, end)).thenReturn(List.of());
        when(transactionRepository.findTop5ByUserIdBetween(
                eq(USER_ID), eq(start), eq(end), any(Pageable.class))).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(start, end);

        assertThat(response.getTotalIncome()).isEqualByComparingTo("500000");
        assertThat(response.getTotalExpense()).isEqualByComparingTo("0");
        assertThat(response.getTransactionCount()).isEqualTo(2L);
    }

    @Test
    void getDashboard_withStartOnly_filtersFromStart() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.getTotalBalanceByUserId(USER_ID)).thenReturn(new BigDecimal("1000000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionTypeFrom(
                USER_ID, TransactionType.INCOME, start)).thenReturn(new BigDecimal("700000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionTypeFrom(
                USER_ID, TransactionType.EXPENSE, start)).thenReturn(new BigDecimal("300000"));
        when(transactionRepository.countByUserIdFrom(USER_ID, start)).thenReturn(3L);
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeFrom(
                USER_ID, TransactionType.INCOME, start)).thenReturn(List.of());
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeFrom(
                USER_ID, TransactionType.EXPENSE, start)).thenReturn(List.of());
        when(transactionRepository.findTop5ByUserIdFrom(
                eq(USER_ID), eq(start), any(Pageable.class))).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(start, null);

        assertThat(response.getTotalIncome()).isEqualByComparingTo("700000");
        assertThat(response.getTotalExpense()).isEqualByComparingTo("300000");
        assertThat(response.getTransactionCount()).isEqualTo(3L);
    }

    @Test
    void getDashboard_withEndOnly_filtersUntilEnd() {
        LocalDateTime end = LocalDateTime.of(2026, 8, 2, 0, 0);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.getTotalBalanceByUserId(USER_ID)).thenReturn(new BigDecimal("1000000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionTypeUntil(
                USER_ID, TransactionType.INCOME, end)).thenReturn(new BigDecimal("900000"));
        when(transactionRepository.getTotalAmountByUserIdAndTransactionTypeUntil(
                USER_ID, TransactionType.EXPENSE, end)).thenReturn(new BigDecimal("100000"));
        when(transactionRepository.countByUserIdUntil(USER_ID, end)).thenReturn(4L);
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeUntil(
                USER_ID, TransactionType.INCOME, end)).thenReturn(List.of());
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionTypeUntil(
                USER_ID, TransactionType.EXPENSE, end)).thenReturn(List.of());
        when(transactionRepository.findTop5ByUserIdUntil(
                eq(USER_ID), eq(end), any(Pageable.class))).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(null, end);

        assertThat(response.getTotalIncome()).isEqualByComparingTo("900000");
        assertThat(response.getTotalExpense()).isEqualByComparingTo("100000");
        assertThat(response.getTransactionCount()).isEqualTo(4L);
    }

    @Test
    void getDashboard_emptyData_returnsZerosAndEmptyLists() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.getTotalBalanceByUserId(USER_ID)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalAmountByUserIdAndTransactionType(
                USER_ID, TransactionType.INCOME)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalAmountByUserIdAndTransactionType(
                USER_ID, TransactionType.EXPENSE)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionType(
                USER_ID, TransactionType.INCOME)).thenReturn(List.of());
        when(transactionRepository.getCategoryTotalsByUserIdAndTransactionType(
                USER_ID, TransactionType.EXPENSE)).thenReturn(List.of());
        when(transactionRepository.findTop5ByUserIdOrderByTransactionDateDesc(USER_ID))
                .thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.getTotalBalance()).isEqualByComparingTo("0");
        assertThat(response.getTotalIncome()).isEqualByComparingTo("0");
        assertThat(response.getTotalExpense()).isEqualByComparingTo("0");
        assertThat(response.getTransactionCount()).isZero();
        assertThat(response.getIncomeByCategory()).isEmpty();
        assertThat(response.getExpenseByCategory()).isEmpty();
        assertThat(response.getRecentTransactions()).isEmpty();
    }
}
