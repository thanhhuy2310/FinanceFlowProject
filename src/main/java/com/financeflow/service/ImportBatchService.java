package com.financeflow.service;

import com.financeflow.dto.request.importbatch.ImportBatchRequest;
import com.financeflow.dto.response.importbatch.ImportBatchResponse;
import com.financeflow.entity.Account;
import com.financeflow.entity.Category;
import com.financeflow.entity.ImportBatch;
import com.financeflow.entity.ImportBatchError;
import com.financeflow.entity.Provider;
import com.financeflow.entity.Rule;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.enums.ImportBatchStatus;
import com.financeflow.enums.TransactionType;
import com.financeflow.exception.CsvImportException;
import com.financeflow.mapper.ImportBatchErrorMapper;
import com.financeflow.mapper.ImportBatchMapper;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.ImportBatchErrorRepository;
import com.financeflow.repository.ImportBatchRepository;
import com.financeflow.repository.ProviderRepository;
import com.financeflow.repository.RuleRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import com.financeflow.service.CsvTransactionParser.CsvRow;
import com.financeflow.service.CsvTransactionParser.CsvRowError;
import com.financeflow.service.CsvTransactionParser.ParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ImportBatchService {

    private final ImportBatchRepository importBatchRepository;
    private final ImportBatchErrorRepository importBatchErrorRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final ProviderRepository providerRepository;
    private final RuleRepository ruleRepository;
    private final ImportBatchMapper importBatchMapper;
    private final ImportBatchErrorMapper importBatchErrorMapper;
    private final CsvTransactionParser csvTransactionParser;
    private final RuleService ruleService;

    @Transactional
    public ImportBatchResponse create(ImportBatchRequest request) {
        ImportBatch importBatch = importBatchMapper.toEntity(request);
        importBatch.setUser(getCurrentUser());
        importBatch.setFileName(request.getFileName().trim());
        importBatch.setImportedAt(LocalDateTime.now());
        importBatch.setTotalRows(0);
        importBatch.setSuccessRows(0);
        importBatch.setFailedRows(0);
        importBatch.setStatus(ImportBatchStatus.PENDING);

        return toResponse(importBatchRepository.save(importBatch));
    }

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> findAll() {
        User currentUser = getCurrentUser();
        return importBatchRepository.findByUserId(currentUser.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportBatchResponse findById(Long id) {
        return toResponse(getOwnedImportBatch(id));
    }

    @Transactional
    public void delete(Long id) {
        importBatchRepository.delete(getOwnedImportBatch(id));
    }

    /**
     * Parses an uploaded CSV file and imports every valid row as a transaction.
     * Rows that cannot be resolved are skipped and recorded as batch failures;
     * the batch is completed with a summary of the outcome.
     */
    @Transactional
    public ImportBatchResponse importCsv(Long id, MultipartFile file) {
        ImportBatch importBatch = getOwnedImportBatch(id);
        if (importBatch.getStatus() != ImportBatchStatus.PENDING) {
            throw new CsvImportException("Only PENDING import batches can be imported");
        }
        validateFile(file);

        ParseResult parseResult = parseFile(file, importBatch);

        User user = importBatch.getUser();
        List<Rule> rules = ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(user.getId());
        List<Category> categories = categoryRepository.findByUserId(user.getId());
        List<Account> accounts = accountRepository.findByUserId(user.getId());
        List<Provider> providers = providerRepository.findAll();

        List<ImportBatchError> failures = new ArrayList<>();
        parseResult.rowErrors().forEach(error ->
                failures.add(toError(importBatch, error.rowNumber(), error.errorMessage())));

        List<Transaction> transactions = new ArrayList<>();
        for (CsvRow row : parseResult.rows()) {
            try {
                transactions.add(buildTransaction(importBatch, user, row, rules, categories, providers, accounts));
            } catch (CsvImportException ex) {
                failures.add(toError(importBatch, row.rowNumber(), ex.getMessage()));
            }
        }

        transactionRepository.saveAll(transactions);
        if (!failures.isEmpty()) {
            importBatchErrorRepository.saveAll(failures);
        }

        importBatch.setTotalRows(transactions.size() + failures.size());
        importBatch.setSuccessRows(transactions.size());
        importBatch.setFailedRows(failures.size());
        importBatch.setErrorMessage(null);
        importBatch.setStatus(ImportBatchStatus.COMPLETED);

        return toResponse(importBatch);
    }

    /**
     * Marks a batch as FAILED with the given reason. Runs in a new transaction so the
     * status survives the rollback of the failing import transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String message) {
        ImportBatch importBatch = getOwnedImportBatch(id);
        importBatch.setStatus(ImportBatchStatus.FAILED);
        importBatch.setErrorMessage(message);
        importBatchRepository.save(importBatch);
    }

    private ParseResult parseFile(MultipartFile file, ImportBatch importBatch) {
        try {
            return csvTransactionParser.parse(file.getInputStream());
        } catch (CsvImportException ex) {
            markFailed(importBatch.getId(), ex.getMessage());
            throw ex;
        } catch (IOException ex) {
            markFailed(importBatch.getId(), "Could not read uploaded file: " + ex.getMessage());
            throw new CsvImportException("Could not read uploaded file: " + ex.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvImportException("Please upload a CSV file");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new CsvImportException("Only .csv files are supported");
        }
    }

    private Transaction buildTransaction(
            ImportBatch importBatch,
            User user,
            CsvRow row,
            List<Rule> rules,
            List<Category> categories,
            List<Provider> providers,
            List<Account> accounts) {

        Category category = resolveCategory(user, row, rules, categories);
        Account account = resolveAccount(row, providers, accounts);
        updateBalance(account, row.amount(), row.transactionType());

        LocalDateTime now = LocalDateTime.now();
        return Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .importBatch(importBatch)
                .amount(row.amount())
                .transactionType(row.transactionType())
                .transactionDate(row.transactionDate())
                .description(row.description())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Category resolveCategory(User user, CsvRow row, List<Rule> rules, List<Category> categories) {
        Category category = ruleService.matchCategory(rules, row.description()).orElse(null);

        if (category == null && row.categoryName() != null) {
            category = categories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(row.categoryName()))
                    .findFirst()
                    .orElseThrow(() -> new CsvImportException("Category not found: " + row.categoryName()));
        }

        if (category == null) {
            throw new CsvImportException("No matching rule or category for this transaction");
        }
        if (category.getType().name().equals(row.transactionType().name())) {
            return category;
        }
        throw new CsvImportException(
                "Category '" + category.getName() + "' is not valid for " + row.transactionType() + " transactions");
    }

    private Account resolveAccount(CsvRow row, List<Provider> providers, List<Account> accounts) {
        if (row.providerName() == null) {
            throw new CsvImportException("Provider is required");
        }

        Provider provider = providers.stream()
                .filter(p -> p.getName().equalsIgnoreCase(row.providerName()))
                .findFirst()
                .orElseThrow(() -> new CsvImportException("Provider not found: " + row.providerName()));

        return accounts.stream()
                .filter(a -> a.getProvider() != null && a.getProvider().getId().equals(provider.getId()))
                .findFirst()
                .orElseThrow(() -> new CsvImportException("No account found for provider: " + row.providerName()));
    }

    private void updateBalance(Account account, BigDecimal amount, TransactionType transactionType) {
        if (transactionType == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            BigDecimal updatedBalance = account.getBalance().subtract(amount);
            if (updatedBalance.signum() < 0) {
                throw new CsvImportException("Insufficient account balance for " + account.getAccountName());
            }
            account.setBalance(updatedBalance);
        }
        account.setUpdatedAt(LocalDateTime.now());
    }

    private ImportBatchError toError(ImportBatch importBatch, int rowNumber, String message) {
        return ImportBatchError.builder()
                .importBatch(importBatch)
                .rowNumber(rowNumber)
                .errorMessage(message)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ImportBatchResponse toResponse(ImportBatch importBatch) {
        ImportBatchResponse response = importBatchMapper.toResponse(importBatch);
        response.setFailures(importBatchErrorMapper.toResponses(
                importBatchErrorRepository.findByImportBatchId(importBatch.getId())));
        return response;
    }

    private ImportBatch getOwnedImportBatch(Long id) {
        User currentUser = getCurrentUser();
        return importBatchRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import batch not found"));
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
