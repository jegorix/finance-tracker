package com.finance.tracker.config;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.AccountType;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Category;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.User;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.demo-data-enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final List<PersonSeed> PEOPLE = List.of(
            new PersonSeed("Olivia Turner", "olivia.turner@example.com"),
            new PersonSeed("Liam Carter", "liam.carter@example.com"),
            new PersonSeed("Emma Hughes", "emma.hughes@example.com"),
            new PersonSeed("Noah Bennett", "noah.bennett@example.com"),
            new PersonSeed("Ava Richardson", "ava.richardson@example.com"),
            new PersonSeed("Mason Price", "mason.price@example.com"),
            new PersonSeed("Sophia Griffin", "sophia.griffin@example.com"),
            new PersonSeed("Ethan Brooks", "ethan.brooks@example.com"),
            new PersonSeed("Isabella Foster", "isabella.foster@example.com"),
            new PersonSeed("James Cooper", "james.cooper@example.com"));

    private static final int MIN_INCOME_CENTS = 250_000;
    private static final int MAX_INCOME_CENTS = 550_000;
    private static final int MIN_EXPENSE_CENTS = 1_500;
    private static final int MAX_EXPENSE_CENTS = 25_000;
    private static final int MIN_TRANSFER_CENTS = 5_000;
    private static final int MAX_TRANSFER_CENTS = 80_000;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Demo seed skipped: users table is not empty.");
            return;
        }

        YearMonth currentMonth = YearMonth.now();
        LocalDate periodStart = currentMonth.atDay(1);
        LocalDate periodEnd = currentMonth.atEndOfMonth();
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Random random = new Random(20_260_427L);

        for (int i = 0; i < PEOPLE.size(); i++) {
            createUserDataset(PEOPLE.get(i), i, periodStart, periodEnd, now, random);
        }

        log.info("Demo seed completed: {} users with related finance data created.", PEOPLE.size());
    }

    private void createUserDataset(
            PersonSeed person,
            int index,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDateTime now,
            Random random) {
        User user = new User();
        user.setUsername(person.fullName());
        user.setEmail(person.email());
        user = userRepository.save(user);

        Account checking = createAccount(user, "Main Checking", AccountType.CHECKING, amount(180_000 + index * 15_000));
        Account savings = createAccount(user, "Emergency Savings", AccountType.SAVINGS, amount(400_000 + index * 30_000));
        Account credit = createAccount(user, "Daily Credit Card", AccountType.CREDIT, amount(10_000 + index * 1_500));
        List<Account> accounts = accountRepository.saveAll(List.of(checking, savings, credit));
        checking = accounts.get(0);
        savings = accounts.get(1);
        credit = accounts.get(2);

        Category housing = createCategory(user, "Housing");
        Category groceries = createCategory(user, "Groceries");
        Category transport = createCategory(user, "Transport");
        Category entertainment = createCategory(user, "Entertainment");
        Category utilities = createCategory(user, "Utilities");
        List<Category> categories = categoryRepository.saveAll(List.of(housing, groceries, transport, entertainment, utilities));

        Map<String, Category> categoriesByName = Map.of(
                "Housing", categories.get(0),
                "Groceries", categories.get(1),
                "Transport", categories.get(2),
                "Entertainment", categories.get(3),
                "Utilities", categories.get(4));

        Budget homeBudget = createBudget(
                user,
                "Home & Bills",
                amount(95_000 + index * 3_000),
                periodStart,
                periodEnd,
                List.of(categoriesByName.get("Housing"), categoriesByName.get("Utilities")));
        Budget essentialsBudget = createBudget(
                user,
                "Daily Essentials",
                amount(55_000 + index * 2_000),
                periodStart,
                periodEnd,
                List.of(categoriesByName.get("Groceries"), categoriesByName.get("Transport")));
        Budget leisureBudget = createBudget(
                user,
                "Leisure",
                amount(28_000 + index * 1_000),
                periodStart,
                periodEnd,
                List.of(categoriesByName.get("Entertainment")));
        List<Budget> budgets = budgetRepository.saveAll(List.of(homeBudget, essentialsBudget, leisureBudget));

        List<Transaction> transactions = new ArrayList<>();
        Budget home = budgets.get(0);
        Budget essentials = budgets.get(1);
        Budget leisure = budgets.get(2);

        transactions.add(createTransaction(now.minusDays(25), randomCents(random, MIN_INCOME_CENTS, MAX_INCOME_CENTS),
                "Monthly salary deposit", TransactionType.INCOME, null, checking));
        transactions.add(createTransaction(now.minusDays(17), randomCents(random, MIN_INCOME_CENTS, MAX_INCOME_CENTS),
                "Quarterly bonus payout", TransactionType.INCOME, null, checking));

        transactions.add(createTransaction(now.minusDays(24), randomCents(random, MIN_EXPENSE_CENTS, MAX_EXPENSE_CENTS),
                "Supermarket groceries", TransactionType.EXPENSE, essentials, checking));
        transactions.add(createTransaction(now.minusDays(20), randomCents(random, MIN_EXPENSE_CENTS, MAX_EXPENSE_CENTS),
                "Apartment rent payment", TransactionType.EXPENSE, home, checking));
        transactions.add(createTransaction(now.minusDays(18), randomCents(random, MIN_EXPENSE_CENTS, MAX_EXPENSE_CENTS),
                "Electricity and water bill", TransactionType.EXPENSE, home, checking));
        transactions.add(createTransaction(now.minusDays(14), randomCents(random, MIN_EXPENSE_CENTS, MAX_EXPENSE_CENTS),
                "Fuel and parking", TransactionType.EXPENSE, essentials, checking));
        transactions.add(createTransaction(now.minusDays(10), randomCents(random, MIN_EXPENSE_CENTS, MAX_EXPENSE_CENTS),
                "Streaming subscription", TransactionType.EXPENSE, leisure, credit));
        transactions.add(createTransaction(now.minusDays(7), randomCents(random, MIN_EXPENSE_CENTS, MAX_EXPENSE_CENTS),
                "Cinema and dinner", TransactionType.EXPENSE, leisure, credit));

        transactions.add(createTransaction(now.minusDays(12), randomCents(random, MIN_TRANSFER_CENTS, MAX_TRANSFER_CENTS),
                "Transfer to savings", TransactionType.TRANSFER, null, savings));
        transactions.add(createTransaction(now.minusDays(3), randomCents(random, MIN_TRANSFER_CENTS, MAX_TRANSFER_CENTS),
                "Card repayment transfer", TransactionType.TRANSFER, null, checking));

        transactionRepository.saveAll(transactions);
    }

    private Account createAccount(User user, String name, AccountType type, BigDecimal balance) {
        Account account = new Account();
        account.setUser(user);
        account.setName(name);
        account.setType(type);
        account.setBalance(balance);
        return account;
    }

    private Category createCategory(User user, String name) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        return category;
    }

    private Budget createBudget(
            User user,
            String name,
            BigDecimal limit,
            LocalDate periodStart,
            LocalDate periodEnd,
            List<Category> categories) {
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setName(name);
        budget.setLimitAmount(limit);
        budget.setPeriodStart(periodStart);
        budget.setPeriodEnd(periodEnd);
        budget.setCategories(categories);
        return budget;
    }

    private Transaction createTransaction(
            LocalDateTime occurredAt,
            BigDecimal amount,
            String description,
            TransactionType type,
            Budget budget,
            Account account) {
        Transaction transaction = new Transaction();
        transaction.setOccurredAt(occurredAt);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setBudget(budget);
        transaction.setAccount(account);
        return transaction;
    }

    private BigDecimal randomCents(Random random, int minInclusive, int maxInclusive) {
        int randomCents = random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
        return amount(randomCents);
    }

    private BigDecimal amount(int cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    private record PersonSeed(String fullName, String email) {
    }
}
