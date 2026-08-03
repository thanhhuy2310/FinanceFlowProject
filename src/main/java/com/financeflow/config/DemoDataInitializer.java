package com.financeflow.config;

import com.financeflow.entity.Account;
import com.financeflow.entity.Category;
import com.financeflow.entity.ImportBatch;
import com.financeflow.entity.ImportBatchError;
import com.financeflow.entity.Provider;
import com.financeflow.entity.Rule;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.enums.AccountType;
import com.financeflow.enums.CategoryType;
import com.financeflow.enums.ImportBatchStatus;
import com.financeflow.enums.TransactionType;
import com.financeflow.enums.UserRole;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.ImportBatchErrorRepository;
import com.financeflow.repository.ImportBatchRepository;
import com.financeflow.repository.ProviderRepository;
import com.financeflow.repository.RuleRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seeds a realistic demo dataset for the dev profile.
 * <p>
 * Runs only when the database has no users yet: 1 demo user, 10 providers,
 * 4 accounts, 10 categories, 15 rules and 200 transactions spread over the
 * last 6 months, plus 2 import batches (one completed, one failed). All
 * randomness is driven by a fixed seed so the dataset is reproducible.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private static final long SEED = 42L;
    private static final String DEMO_EMAIL = "demo@financeflow.com";
    private static final String DEMO_PASSWORD = "demo123456";

    private static final List<String> EXTRA_PROVIDERS = List.of("VPBank", "VNPT Pay");

    private static final List<String> FOOD_DESCRIPTIONS = List.of(
            "The Coffee House", "Highlands Coffee", "Phuc Long", "Cafe Trung Nguyen",
            "KFC", "Lotteria", "Vinmart", "Co.opmart");
    private static final List<String> TRANSPORT_DESCRIPTIONS = List.of(
            "Grab Car", "Grab Bike", "Xanh SM Taxi", "Be - Taxi");
    private static final List<String> SHOPPING_DESCRIPTIONS = List.of(
            "Shopee - Mua sam", "Lazada - Dien tu", "Tiki - Sach", "Uniqlo", "Zara");
    private static final List<String> UTILITIES_DESCRIPTIONS = List.of(
            "EVN Ha Noi - Tien dien", "VNPT - Internet", "Viettel - Cuoc di dong");
    private static final List<String> HEALTH_DESCRIPTIONS = List.of(
            "Pharmacity", "Long Chau Pharmacy", "Benh vien Bach Mai");
    private static final List<String> ENTERTAINMENT_DESCRIPTIONS = List.of(
            "Netflix", "Spotify", "CGV Cinema", "Steam", "Galaxy Play");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final ImportBatchRepository importBatchRepository;
    private final ImportBatchErrorRepository importBatchErrorRepository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Random random = new Random(SEED);
        LocalDateTime now = LocalDateTime.now();

        User user = userRepository.findByEmail(DEMO_EMAIL).orElseGet(() -> createUser(now));

        Map<String, Provider> providers;
        if (providerRepository.count() == 0) {
            providers = createProviders(now);
        } else {
            providers = new HashMap<>();
            providerRepository.findAll().forEach(provider -> providers.put(provider.getName().toLowerCase(), provider));
            for (String name : EXTRA_PROVIDERS) {
                if (!providers.containsKey(name.toLowerCase())) {
                    Provider provider = providerRepository.save(Provider.builder()
                            .name(name)
                            .logoUrl("/asset/providers/" + name.toLowerCase().replace(" ", "") + ".svg")
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                    providers.put(name.toLowerCase(), provider);
                }
            }
        }

        List<Category> categories;
        if (categoryRepository.count() == 0) {
            categories = createCategories(user, now);
        } else {
            categories = categoryRepository.findByUserId(user.getId());
            if (categories.isEmpty()) {
                categories = createCategories(user, now);
            }
        }

        List<Rule> rules;
        if (ruleRepository.count() == 0) {
            rules = createRules(user, categories, now);
        } else {
            rules = ruleRepository.findByUserId(user.getId());
            if (rules.isEmpty()) {
                rules = createRules(user, categories, now);
            }
        }

        List<Account> accounts;
        if (accountRepository.count() == 0) {
            accounts = createAccounts(user, providers, now);
        } else {
            accounts = accountRepository.findByUserId(user.getId());
            if (accounts.isEmpty()) {
                accounts = createAccounts(user, providers, now);
            }
        }

        Map<Account, BigDecimal> balances = new HashMap<>();
        accounts.forEach(account -> balances.put(account, account.getBalance()));

        List<ImportBatch> batches;
        if (importBatchRepository.count() == 0) {
            batches = createBatches(user, now);
        } else {
            batches = importBatchRepository.findByUserId(user.getId());
            if (batches.isEmpty()) {
                batches = createBatches(user, now);
            }
        }

        if (transactionRepository.count() == 0) {
            List<Transaction> transactions = new ArrayList<>();
            int batchCursor = 0;

            for (int monthIndex = 5; monthIndex >= 0; monthIndex--) {
                LocalDate firstDay = LocalDate.now().withDayOfMonth(1).minusMonths(monthIndex);
                LocalDate month = firstDay.plusMonths(1);

                Transaction salary = buildTransaction(user, accounts.get(0), category("Salary", categories),
                        firstDay.atTime(LocalTime.of(9, 0)), 25_000_000, "Salary - ABC Corp",
                        monthIndex >= 4 && batchCursor < 40 ? batches.get(0) : null);
                transactions.add(salary);
                applyBalance(balances, accounts.get(0), salary);

                if (monthIndex % 2 == 0) {
                    Transaction bonus = buildTransaction(user, accounts.get(0), category("Bonus", categories),
                            firstDay.plusDays(random.nextInt(1, 16)).atTime(LocalTime.of(10, 0)),
                            5_000_000 + random.nextInt(0, 10_000_000) / 1000 * 1000,
                            "Bonus KPI", null);
                    transactions.add(bonus);
                    applyBalance(balances, accounts.get(0), bonus);
                }

                if (monthIndex % 3 == 0) {
                    Transaction interest = buildTransaction(user, accounts.get(1), category("Investment", categories),
                            firstDay.plusDays(random.nextInt(1, 11)).atTime(LocalTime.of(8, 0)),
                            200_000 + random.nextInt(0, 300_000) / 1000 * 1000,
                            "VPBank - Lai suat tiet kiem", null);
                    transactions.add(interest);
                    applyBalance(balances, accounts.get(1), interest);
                }

                int expenseCount = monthIndex == 0 ? 29 : 32;
                for (int i = 0; i < expenseCount; i++) {
                    int day = random.nextInt(2, 29);
                    LocalTime time = LocalTime.of(random.nextInt(7, 23), random.nextInt(0, 60));
                    LocalDateTime date = firstDay.plusDays(day - 1).atTime(time);

                    Account account = accounts.get(random.nextInt(accounts.size()));
                    Category category = weightedExpenseCategory(categories, random);
                    long amount = expenseAmount(category, random);
                    amount = Math.min(amount, balances.get(account).multiply(BigDecimal.valueOf(8L))
                            .divide(BigDecimal.valueOf(10L), 0, java.math.RoundingMode.DOWN).longValue());
                    amount = Math.max(amount, 10_000);

                    String description = descriptionFor(category, random);
                    ImportBatch batch = batchCursor < 40 ? batches.get(0) : null;
                    transactions.add(buildTransaction(user, account, category, date, amount, description, batch));
                    batchCursor++;
                }
            }

            accounts.forEach(account -> account.setBalance(balances.get(account)));
            accountRepository.saveAll(accounts);
            transactionRepository.saveAll(transactions);

            log.info("Demo data initialized: user={}, accounts={}, categories={}, rules={}, "
                            + "providers={}, transactions={}, importBatches={}",
                    user.getEmail(), accounts.size(), categories.size(), rules.size(),
                    providers.size(), transactions.size(), batches.size());
        } else {
            log.info("Demo data already present, nothing to seed (user={})", user.getEmail());
        }
    }

    private User createUser(LocalDateTime now) {
        LocalDateTime created = now.minusMonths(6);
        return userRepository.save(User.builder()
                .fullName("Nguyen Van An")
                .email(DEMO_EMAIL)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .role(UserRole.USER)
                .status(true)
                .createdAt(created)
                .updatedAt(created)
                .build());
    }

    private Map<String, Provider> createProviders(LocalDateTime now) {
        Map<String, Provider> providers = new HashMap<>();
        providerRepository.findAll().forEach(provider -> providers.put(provider.getName().toLowerCase(), provider));

        for (String name : EXTRA_PROVIDERS) {
            if (!providers.containsKey(name.toLowerCase())) {
                Provider provider = providerRepository.save(Provider.builder()
                        .name(name)
                        .logoUrl("/asset/providers/" + name.toLowerCase().replace(" ", "") + ".svg")
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
                providers.put(name.toLowerCase(), provider);
            }
        }
        return providers;
    }

    private List<Category> createCategories(User user, LocalDateTime now) {
        List<Category> categories = new ArrayList<>();
        categories.add(category(user, "Salary", CategoryType.INCOME, "briefcase", "#10B981", now));
        categories.add(category(user, "Bonus", CategoryType.INCOME, "gift", "#F472B6", now));
        categories.add(category(user, "Investment", CategoryType.INCOME, "trending-up", "#06B6D4", now));
        categories.add(category(user, "Other Income", CategoryType.INCOME, "coins", "#22C55E", now));
        categories.add(category(user, "Food & Drink", CategoryType.EXPENSE, "utensils", "#F59E0B", now));
        categories.add(category(user, "Transport", CategoryType.EXPENSE, "car", "#3B82F6", now));
        categories.add(category(user, "Shopping", CategoryType.EXPENSE, "shopping-bag", "#EC4899", now));
        categories.add(category(user, "Utilities", CategoryType.EXPENSE, "zap", "#F97316", now));
        categories.add(category(user, "Health", CategoryType.EXPENSE, "heart-pulse", "#EF4444", now));
        categories.add(category(user, "Entertainment", CategoryType.EXPENSE, "film", "#8B5CF6", now));
        return categoryRepository.saveAll(categories);
    }

    private Category category(User user, String name, CategoryType type, String icon, String color, LocalDateTime now) {
        return Category.builder()
                .user(user)
                .name(name)
                .type(type)
                .icon(icon)
                .color(color)
                .isDefault(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private List<Rule> createRules(User user, List<Category> categories, LocalDateTime now) {
        String[][] definitions = {
                {"salary", "Salary"},
                {"luong", "Salary"},
                {"bonus", "Bonus"},
                {"thuong", "Bonus"},
                {"grab", "Transport"},
                {"be", "Transport"},
                {"xanh sm", "Transport"},
                {"shopee", "Shopping"},
                {"lazada", "Shopping"},
                {"tiki", "Shopping"},
                {"coffee", "Food & Drink"},
                {"evn", "Utilities"},
                {"vnpt", "Utilities"},
                {"viettel", "Utilities"},
                {"netflix", "Entertainment"},
        };

        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < definitions.length; i++) {
            rules.add(Rule.builder()
                    .user(user)
                    .category(category(definitions[i][1], categories))
                    .keyword(definitions[i][0])
                    .priority(i + 1)
                    .isActive(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        return ruleRepository.saveAll(rules);
    }

    private List<Account> createAccounts(User user, Map<String, Provider> providers, LocalDateTime now) {
        List<Account> accounts = List.of(
                account(user, providers, "Techcombank", "Techcombank Checking", "9704070000001234",
                        AccountType.BANK, 25_000_000, now),
                account(user, providers, "VPBank", "VPBank Savings", "8964000000005678",
                        AccountType.BANK, 80_000_000, now),
                account(user, providers, "MoMo", "MoMo Wallet", "0987654321",
                        AccountType.EWALLET, 5_000_000, now),
                account(user, providers, "ZaloPay", "ZaloPay Wallet", "0901234567",
                        AccountType.EWALLET, 3_000_000, now));
        return accountRepository.saveAll(accounts);
    }

    private Account account(User user, Map<String, Provider> providers, String providerName,
                            String accountName, String accountNumber, AccountType type,
                            long balance, LocalDateTime now) {
        return Account.builder()
                .user(user)
                .provider(providers.get(providerName.toLowerCase()))
                .accountName(accountName)
                .accountNumber(accountNumber)
                .accountType(type)
                .balance(BigDecimal.valueOf(balance))
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private List<ImportBatch> createBatches(User user, LocalDateTime now) {
        ImportBatch completed = ImportBatch.builder()
                .user(user)
                .fileName("demo-transactions.csv")
                .importedAt(now.minusMonths(6))
                .totalRows(40)
                .successRows(40)
                .failedRows(0)
                .status(ImportBatchStatus.COMPLETED)
                .build();
        importBatchRepository.save(completed);

        ImportBatch failed = ImportBatch.builder()
                .user(user)
                .fileName("broken-import.csv")
                .importedAt(now.minusMonths(1))
                .totalRows(2)
                .successRows(0)
                .failedRows(2)
                .status(ImportBatchStatus.FAILED)
                .errorMessage("Invalid CSV header. Expected: date,description,amount,type,category,provider")
                .build();
        importBatchRepository.save(failed);

        importBatchErrorRepository.saveAll(List.of(
                ImportBatchError.builder()
                        .importBatch(failed)
                        .rowNumber(2)
                        .errorMessage("Provider not found: Vietcombank")
                        .createdAt(now.minusMonths(1))
                        .build(),
                ImportBatchError.builder()
                        .importBatch(failed)
                        .rowNumber(3)
                        .errorMessage("Invalid date: 30/02/2026 (expected yyyy-MM-dd)")
                        .createdAt(now.minusMonths(1))
                        .build()));

        return List.of(completed, failed);
    }

    private Transaction buildTransaction(User user, Account account, Category category,
                                         LocalDateTime date, long amount, String description,
                                         ImportBatch batch) {
        return Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .importBatch(batch)
                .amount(BigDecimal.valueOf(amount))
                .transactionType(TransactionType.valueOf(category.getType().name()))
                .transactionDate(date)
                .description(description)
                .createdAt(date)
                .updatedAt(date)
                .build();
    }

    private void applyBalance(Map<Account, BigDecimal> balances, Account account, Transaction transaction) {
        BigDecimal balance = balances.get(account);
        balance = transaction.getTransactionType() == TransactionType.INCOME
                ? balance.add(transaction.getAmount())
                : balance.subtract(transaction.getAmount());
        balances.put(account, balance);
    }

    private Category weightedExpenseCategory(List<Category> categories, Random random) {
        return switch (random.nextInt(10)) {
            case 0, 1, 2 -> category("Food & Drink", categories);
            case 3, 4 -> category("Transport", categories);
            case 5 -> category("Shopping", categories);
            case 6 -> category("Utilities", categories);
            case 7 -> category("Health", categories);
            default -> category("Entertainment", categories);
        };
    }

    private long expenseAmount(Category category, Random random) {
        return switch (category.getName()) {
            case "Food & Drink" -> 20_000 + random.nextInt(0, 480_000) / 1000 * 1000;
            case "Transport" -> 15_000 + random.nextInt(0, 285_000) / 5000 * 5000;
            case "Shopping" -> 50_000 + random.nextInt(0, 2_950_000) / 10_000 * 10_000;
            case "Utilities" -> 100_000 + random.nextInt(0, 1_400_000) / 10_000 * 10_000;
            case "Health" -> 50_000 + random.nextInt(0, 1_950_000) / 10_000 * 10_000;
            default -> 50_000 + random.nextInt(0, 550_000) / 5000 * 5000;
        };
    }

    private String descriptionFor(Category category, Random random) {
        List<String> pool = switch (category.getName()) {
            case "Food & Drink" -> FOOD_DESCRIPTIONS;
            case "Transport" -> TRANSPORT_DESCRIPTIONS;
            case "Shopping" -> SHOPPING_DESCRIPTIONS;
            case "Utilities" -> UTILITIES_DESCRIPTIONS;
            case "Health" -> HEALTH_DESCRIPTIONS;
            default -> ENTERTAINMENT_DESCRIPTIONS;
        };
        return pool.get(random.nextInt(pool.size()));
    }

    private Category category(String name, List<Category> categories) {
        return categories.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing category: " + name));
    }
}
