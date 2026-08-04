package com.financeflow.service;

import com.financeflow.dto.request.importbatch.ImportBatchRequest;
import com.financeflow.dto.response.importbatch.ImportBatchResponse;
import com.financeflow.entity.Account;
import com.financeflow.entity.Category;
import com.financeflow.entity.ImportBatch;
import com.financeflow.entity.ImportBatchError;
import com.financeflow.entity.Provider;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.enums.CategoryType;
import com.financeflow.enums.ImportBatchStatus;
import com.financeflow.exception.CsvImportException;
import com.financeflow.mapper.ImportBatchErrorMapperImpl;
import com.financeflow.mapper.ImportBatchMapperImpl;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.ImportBatchErrorRepository;
import com.financeflow.repository.ImportBatchRepository;
import com.financeflow.repository.ProviderRepository;
import com.financeflow.repository.RuleRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportBatchServiceTest {

    private static final String USER_EMAIL = "user@financeflow.com";
    private static final Long USER_ID = 1L;
    private static final Long BATCH_ID = 10L;

    @Mock
    private ImportBatchRepository importBatchRepository;
    @Mock
    private ImportBatchErrorRepository importBatchErrorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private RuleRepository ruleRepository;
    @Mock
    private RuleService ruleService;

    @Spy
    private ImportBatchMapperImpl importBatchMapper = new ImportBatchMapperImpl();
    @Spy
    private ImportBatchErrorMapperImpl importBatchErrorMapper = new ImportBatchErrorMapperImpl();
    @Spy
    private CsvTransactionParser csvTransactionParser = new CsvTransactionParser();

    @InjectMocks
    private ImportBatchService importBatchService;

    private final List<ImportBatchError> savedErrors = new ArrayList<>();

    private User user;
    private Provider techcombank;
    private Account account;
    private Category food;
    private Category transport;
    private Category salary;
    private List<String> existingReferences = List.of();

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email(USER_EMAIL).fullName("Demo User").build();

        techcombank = Provider.builder().id(1L).name("Techcombank").build();
        account = Account.builder()
                .id(1L)
                .user(user)
                .provider(techcombank)
                .accountName("Techcombank Checking")
                .balance(new BigDecimal("10000000"))
                .build();
        food = Category.builder().id(1L).user(user).name("Food").type(CategoryType.EXPENSE).build();
        transport = Category.builder().id(2L).user(user).name("Transport").type(CategoryType.EXPENSE).build();
        salary = Category.builder().id(3L).user(user).name("Salary").type(CategoryType.INCOME).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_EMAIL, null, List.of()));

        lenient().when(importBatchErrorRepository.saveAll(any())).thenAnswer(invocation -> {
            savedErrors.addAll(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        lenient().when(importBatchErrorRepository.findByImportBatchId(any()))
                .thenAnswer(invocation -> List.copyOf(savedErrors));
        lenient().when(transactionRepository.findReferenceByUserId(USER_ID))
                .thenAnswer(invocation -> existingReferences);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ImportBatch pendingBatch() {
        return ImportBatch.builder()
                .id(BATCH_ID)
                .user(user)
                .fileName("import.csv")
                .status(ImportBatchStatus.PENDING)
                .totalRows(0)
                .successRows(0)
                .failedRows(0)
                .build();
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file", "transactions.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private void stubOwnershipAndLookup(ImportBatch batch) {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(importBatchRepository.findByIdAndUserId(BATCH_ID, USER_ID)).thenReturn(Optional.of(batch));
    }

    @Test
    void create_setsPendingBatchWithZeroCounters() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(importBatchRepository.save(any(ImportBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportBatchResponse response = importBatchService.create(
                ImportBatchRequest.builder().fileName("  import.csv  ").build());

        assertThat(response.getFileName()).isEqualTo("import.csv");
        assertThat(response.getStatus()).isEqualTo(ImportBatchStatus.PENDING);
        assertThat(response.getTotalRows()).isZero();
        assertThat(response.getSuccessRows()).isZero();
        assertThat(response.getFailedRows()).isZero();
    }

    @Test
    void findById_otherUsersBatch_throwsNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(importBatchRepository.findByIdAndUserId(BATCH_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importBatchService.findById(BATCH_ID))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void importCsv_allRowsValid_completesBatchAndSavesTransactions() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(food, transport, salary));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
        when(providerRepository.findAll()).thenReturn(List.of(techcombank));
        when(ruleService.matchCategory(any(), any())).thenReturn(Optional.empty());

        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,-65000,EXPENSE,Food,Techcombank
                2026-08-02,Grab Car,80000,EXPENSE,Transport,Techcombank
                2026-08-03,Salary,15000000,INCOME,Salary,Techcombank
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        assertThat(batch.getStatus()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(batch.getTotalRows()).isEqualTo(3);
        assertThat(batch.getSuccessRows()).isEqualTo(3);
        assertThat(batch.getFailedRows()).isZero();
        assertThat(response.getFailures()).isEmpty();
        assertThat(account.getBalance()).isEqualByComparingTo(
                new BigDecimal("10000000").subtract(new BigDecimal("65000"))
                        .subtract(new BigDecimal("80000")).add(new BigDecimal("15000000")));

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        List<Transaction> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getImportBatch()).isEqualTo(batch);
        assertThat(saved.get(0).getCategory().getName()).isEqualTo("Food");
        assertThat(saved.get(1).getCategory().getName()).isEqualTo("Transport");
    }

    @Test
    void importCsv_rowFailures_recordedAndBatchStillCompletes() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(food));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
        when(providerRepository.findAll()).thenReturn(List.of(techcombank));
        when(ruleService.matchCategory(any(), any())).thenReturn(Optional.empty());

        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,50000,EXPENSE,Food,Techcombank
                2026-08-02,Shopping,50000,EXPENSE,Unknown,Techcombank
                2026-08-03,Snacks,abc,EXPENSE,Food,Techcombank
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        assertThat(batch.getStatus()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(batch.getTotalRows()).isEqualTo(3);
        assertThat(batch.getSuccessRows()).isEqualTo(1);
        assertThat(batch.getFailedRows()).isEqualTo(2);
        assertThat(response.getFailures()).hasSize(2);
        assertThat(response.getFailures().get(0).getRowNumber()).isEqualTo(4);
        assertThat(response.getFailures().get(0).getErrorMessage()).contains("Invalid amount");
        assertThat(response.getFailures().get(1).getErrorMessage()).contains("does not exist");
        assertThat(response.getFailures().get(1).getDescription()).isEqualTo("Shopping");
        assertThat(response.getFailures().get(1).getCategoryName()).isEqualTo("Unknown");

        ArgumentCaptor<List<ImportBatchError>> errorCaptor = ArgumentCaptor.forClass(List.class);
        verify(importBatchErrorRepository).saveAll(errorCaptor.capture());
        assertThat(errorCaptor.getValue()).hasSize(2);
        assertThat(errorCaptor.getValue().get(0).getImportBatch()).isEqualTo(batch);
    }

    @Test
    void importCsv_ruleMatchingUsedBeforeCategoryColumn() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(food, transport));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
        when(providerRepository.findAll()).thenReturn(List.of(techcombank));
        when(ruleService.matchCategory(any(), any())).thenReturn(Optional.of(transport));

        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Grab Car,80000,EXPENSE,Food,Techcombank
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getCategory().getName()).isEqualTo("Transport");
        assertThat(response.getSuccessRows()).isEqualTo(1);
        assertThat(response.getFailedRows()).isZero();
    }

    @Test
    void importCsv_nonPendingBatch_throws() {
        ImportBatch batch = pendingBatch();
        batch.setStatus(ImportBatchStatus.COMPLETED);
        stubOwnershipAndLookup(batch);

        assertThatThrownBy(() -> importBatchService.importCsv(BATCH_ID, csvFile("date,description,amount,type,category,provider\n")))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void importCsv_wrongFileExtension_throws() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);

        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> importBatchService.importCsv(BATCH_ID, file))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining(".csv");
    }

    @Test
    void importCsv_emptyFile_throwsValidationError() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);

        assertThatThrownBy(() -> importBatchService.importCsv(BATCH_ID, csvFile("")))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("upload a CSV file");

        assertThat(batch.getStatus()).isEqualTo(ImportBatchStatus.PENDING);
    }

    @Test
    void importCsv_invalidHeader_marksBatchFailed() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);

        assertThatThrownBy(() -> importBatchService.importCsv(BATCH_ID, csvFile("a,b,c\n1,2,3")))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("Invalid CSV header");

        assertThat(batch.getStatus()).isEqualTo(ImportBatchStatus.FAILED);
    }

    @Test
    void importCsv_categoryTypeMismatch_marksRowFailed() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(food));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
        when(providerRepository.findAll()).thenReturn(List.of(techcombank));
        when(ruleService.matchCategory(any(), any())).thenReturn(Optional.empty());

        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,50000,INCOME,Food,Techcombank
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        assertThat(batch.getSuccessRows()).isZero();
        assertThat(batch.getFailedRows()).isEqualTo(1);
        assertThat(response.getFailures().get(0).getErrorMessage())
                .contains("not valid for INCOME");
    }

    @Test
    void importCsv_missingProvider_failsRow() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(food));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
        when(providerRepository.findAll()).thenReturn(List.of(techcombank));
        when(ruleService.matchCategory(any(), any())).thenReturn(Optional.empty());

        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,50000,EXPENSE,Food,
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        assertThat(batch.getFailedRows()).isEqualTo(1);
        assertThat(response.getFailures().get(0).getErrorMessage()).contains("Provider is required");
    }

    @Test
    void importCsv_insufficientBalance_failsRow() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(food));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
        when(providerRepository.findAll()).thenReturn(List.of(techcombank));
        when(ruleService.matchCategory(any(), any())).thenReturn(Optional.empty());

        String csv = """
                date,description,amount,type,category,provider
                2026-08-01,Coffee,999999999,EXPENSE,Food,Techcombank
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        assertThat(batch.getFailedRows()).isEqualTo(1);
        assertThat(response.getFailures().get(0).getErrorMessage()).contains("Insufficient account balance");
    }

    @Test
    void importCsv_duplicateReference_skipsRow() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        existingReferences = List.of("REF-2026-001");

        String csv = """
                date,description,amount,type,category,provider,reference
                2026-08-01,Coffee,50000,EXPENSE,Food,Techcombank,REF-2026-001
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        assertThat(batch.getStatus()).isEqualTo(ImportBatchStatus.COMPLETED);
        assertThat(batch.getTotalRows()).isEqualTo(1);
        assertThat(batch.getSuccessRows()).isZero();
        assertThat(batch.getSkippedRows()).isEqualTo(1);
        assertThat(batch.getFailedRows()).isZero();
        assertThat(response.getSkippedRows()).isEqualTo(1);
        assertThat(account.getBalance()).isEqualByComparingTo("10000000");
    }

    @Test
    void importCsv_duplicateWithinSameFile_skipsLaterRow() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(List.of(food));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
        when(providerRepository.findAll()).thenReturn(List.of(techcombank));
        when(ruleService.matchCategory(any(), any())).thenReturn(Optional.empty());

        String csv = """
                date,description,amount,type,category,provider,reference
                2026-08-01,Coffee,50000,EXPENSE,Food,Techcombank,REF-2026-002
                2026-08-02,Coffee 2,60000,EXPENSE,Food,Techcombank,REF-2026-002
                """;

        ImportBatchResponse response = importBatchService.importCsv(BATCH_ID, csvFile(csv));

        assertThat(batch.getTotalRows()).isEqualTo(2);
        assertThat(batch.getSuccessRows()).isEqualTo(1);
        assertThat(batch.getSkippedRows()).isEqualTo(1);
        assertThat(response.getFailures()).isEmpty();
    }

    @Test
    void delete_removesOwnedBatch() {
        ImportBatch batch = pendingBatch();
        stubOwnershipAndLookup(batch);

        importBatchService.delete(BATCH_ID);

        verify(importBatchRepository).delete(batch);
    }
}
