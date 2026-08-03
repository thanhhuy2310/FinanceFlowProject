package com.financeflow.service;

import com.financeflow.dto.request.transaction.TransactionRequest;
import com.financeflow.dto.response.transaction.TransactionResponse;
import com.financeflow.entity.Account;
import com.financeflow.entity.Category;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.enums.CategoryType;
import com.financeflow.enums.TransactionType;
import com.financeflow.mapper.TransactionMapper;
import com.financeflow.mapper.TransactionMapperImpl;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String USER_EMAIL = "user@financeflow.com";
    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long CATEGORY_ID = 20L;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @Spy
    private TransactionMapper transactionMapper = new TransactionMapperImpl();

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .fullName("Demo User")
                .email(USER_EMAIL)
                .build();

        account = Account.builder()
                .id(ACCOUNT_ID)
                .user(user)
                .accountName("Techcombank Checking")
                .balance(new BigDecimal("1000000"))
                .build();

        category = Category.builder()
                .id(CATEGORY_ID)
                .user(user)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_EMAIL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TransactionRequest request(BigDecimal amount, TransactionType type) {
        return TransactionRequest.builder()
                .amount(amount)
                .description("Coffee")
                .transactionDate(LocalDateTime.of(2026, 8, 1, 10, 0))
                .transactionType(type)
                .accountId(ACCOUNT_ID)
                .categoryId(CATEGORY_ID)
                .build();
    }

    @Test
    void create_income_addsAmountToBalance() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.create(request(new BigDecimal("500000"), TransactionType.INCOME));

        assertThat(response.getAmount()).isEqualByComparingTo("500000");
        assertThat(account.getBalance()).isEqualByComparingTo("1500000");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void create_expense_subtractsAmountFromBalance() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.create(request(new BigDecimal("300000"), TransactionType.EXPENSE));

        assertThat(account.getBalance()).isEqualByComparingTo("700000");
    }

    @Test
    void create_expenseExceedingBalance_throwsConflict() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> transactionService.create(request(new BigDecimal("2000000"), TransactionType.EXPENSE)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient account balance");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void create_accountNotFound_throwsNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(request(new BigDecimal("1000"), TransactionType.EXPENSE)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void create_categoryNotFound_throwsNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(request(new BigDecimal("1000"), TransactionType.EXPENSE)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void create_repositoryFailure_propagates() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new RuntimeException("database down"));

        assertThatThrownBy(() -> transactionService.create(request(new BigDecimal("1000"), TransactionType.INCOME)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("database down");
    }

    @Test
    void delete_expense_addsAmountBackToBalance() {
        Transaction transaction = Transaction.builder()
                .id(1L)
                .user(user)
                .account(account)
                .category(category)
                .amount(new BigDecimal("300000"))
                .transactionType(TransactionType.EXPENSE)
                .build();
        account.setBalance(new BigDecimal("700000"));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(transaction));

        transactionService.delete(1L);

        assertThat(account.getBalance()).isEqualByComparingTo("1000000");
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void delete_income_subtractsAmountFromBalance() {
        Transaction transaction = Transaction.builder()
                .id(1L)
                .user(user)
                .account(account)
                .category(category)
                .amount(new BigDecimal("400000"))
                .transactionType(TransactionType.INCOME)
                .build();
        account.setBalance(new BigDecimal("1000000"));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(transaction));

        transactionService.delete(1L);

        assertThat(account.getBalance()).isEqualByComparingTo("600000");
    }

    @Test
    void delete_incomeMakingBalanceNegative_throwsConflict() {
        Transaction transaction = Transaction.builder()
                .id(1L)
                .user(user)
                .account(account)
                .category(category)
                .amount(new BigDecimal("1500000"))
                .transactionType(TransactionType.INCOME)
                .build();
        account.setBalance(new BigDecimal("1000000"));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.delete(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("balance would become negative");

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void delete_transactionNotFound_throwsNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void findById_mapsAccountAndCategoryNames() {
        Transaction transaction = Transaction.builder()
                .id(1L)
                .user(user)
                .account(account)
                .category(category)
                .amount(new BigDecimal("300000"))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.of(2026, 8, 1, 10, 0))
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.findById(1L);

        assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getAccountName()).isEqualTo("Techcombank Checking");
        assertThat(response.getCategoryName()).isEqualTo("Food");
    }

    @Test
    void findAll_returnsMappedTransactions() {
        Transaction transaction = Transaction.builder()
                .id(1L)
                .user(user)
                .account(account)
                .category(category)
                .amount(new BigDecimal("300000"))
                .transactionType(TransactionType.EXPENSE)
                .transactionDate(LocalDateTime.of(2026, 8, 1, 10, 0))
                .description("Coffee")
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(USER_ID))
                .thenReturn(List.of(transaction));

        List<TransactionResponse> responses = transactionService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getDescription()).isEqualTo("Coffee");
        assertThat(responses.get(0).getAmount()).isEqualByComparingTo("300000");
    }
}
