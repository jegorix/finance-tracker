package com.finance.tracker.service.impl;

import java.math.BigDecimal;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.cache.TransactionSearchCacheKey;
import com.finance.tracker.cache.TransactionSearchIndex;
import com.finance.tracker.cache.TransactionSearchIndexInvalidator;
import com.finance.tracker.dto.request.TransactionSearchQueryMode;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.dto.response.TransactionSearchResult;
import com.finance.tracker.dto.response.TransactionSearchSource;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.service.TransactionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE_PREFIX = "Transaction not found ";
    private static final BigDecimal SEARCH_MIN_AMOUNT = new BigDecimal("-999999999999.99");
    private static final BigDecimal SEARCH_MAX_AMOUNT = new BigDecimal("999999999999.99");
    private static final LocalDateTime SEARCH_START_DATE_TIME = LocalDateTime.of(1, 1, 1, 0, 0);
    private static final LocalDateTime SEARCH_END_DATE_TIME = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    private static final String BULK_REQUEST_EMPTY_MESSAGE =
            "Bulk transaction request must contain at least one item";

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionSearchIndex transactionSearchIndex;
    private final TransactionSearchIndexInvalidator transactionSearchIndexInvalidator;

    @Override
    public TransactionResponse findById(Long id) {
        return transactionMapper.toResponse(getTransaction(id), true, true);
    }

    @Override
    public List<TransactionResponse> findAll(boolean withEntityGraph) {
        List<Transaction> transactions = withEntityGraph
                ? transactionRepository.findAllTransactionsWithEntityGraph()
                : transactionRepository.findAllTransactions();
        return toResponses(transactions);
    }

    @Override
    public List<TransactionResponse> findByDateRange(LocalDateTime startDateTime,
            LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw new BadRequestException("Both startDateTime and endDateTime are required for date range filtering");
        }

        if (endDateTime.isBefore(startDateTime)) {
            throw new BadRequestException("endDateTime must be greater than or equal to startDateTime");
        }

        List<Transaction> transactions = transactionRepository.findByOccurredAtBetween(startDateTime, endDateTime);
        return toResponses(transactions);
    }

    @Override
    public TransactionSearchResult search(
            TransactionSearchQueryMode queryMode,
            String budgetName,
            String accountName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable) {
        validateSearchFilters(minAmount, maxAmount, startDateTime, endDateTime);

        TransactionSearchQueryMode normalizedQueryMode = queryMode == null
                ? TransactionSearchQueryMode.JPQL
                : queryMode;
        Pageable repositoryPageable = normalizePageableForQuery(normalizedQueryMode, pageable);
        String normalizedBudgetName = normalizeTextFilterForCache(budgetName);
        String normalizedAccountName = normalizeTextFilterForCache(accountName);
        String repositoryBudgetName = normalizeTextFilterForRepository(budgetName);
        String repositoryAccountName = normalizeTextFilterForRepository(accountName);
        BigDecimal repositoryMinAmount = minAmount == null ? SEARCH_MIN_AMOUNT : minAmount;
        BigDecimal repositoryMaxAmount = maxAmount == null ? SEARCH_MAX_AMOUNT : maxAmount;
        LocalDateTime repositoryStartDateTime = startDateTime == null ? SEARCH_START_DATE_TIME : startDateTime;
        LocalDateTime repositoryEndDateTime = endDateTime == null ? SEARCH_END_DATE_TIME : endDateTime;

        TransactionSearchCacheKey cacheKey = new TransactionSearchCacheKey(
                normalizedQueryMode,
                normalizedBudgetName,
                normalizedAccountName,
                minAmount,
                maxAmount,
                startDateTime,
                endDateTime,
                repositoryPageable.getPageNumber(),
                repositoryPageable.getPageSize(),
                repositoryPageable.getSort().toString());
        String searchLogContext = buildSearchLogContext(
                normalizedQueryMode,
                normalizedBudgetName,
                normalizedAccountName,
                minAmount,
                maxAmount,
                startDateTime,
                endDateTime,
                repositoryPageable);

        return transactionSearchIndex.find(cacheKey)
                .map(page -> {
                    log.info("Transaction search cache HIT [{}]", searchLogContext);
                    return new TransactionSearchResult(page, TransactionSearchSource.CACHE);
                })
                .orElseGet(() -> loadAndIndexSearchResult(
                        cacheKey,
                        normalizedQueryMode,
                        repositoryBudgetName,
                        repositoryAccountName,
                        repositoryMinAmount,
                        repositoryMaxAmount,
                        repositoryStartDateTime,
                        repositoryEndDateTime,
                        repositoryPageable,
                        pageable,
                        searchLogContext));
    }

    @Override
    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        return transactionMapper.toResponse(createTransactionEntity(request));
    }

    @Override
    @Transactional
    public List<TransactionResponse> createBulkTx(List<TransactionRequest> requests) {
        return createBulkInternal(requests);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<TransactionResponse> createBulkNoTx(List<TransactionRequest> requests) {
        return createBulkInternal(requests);
    }

    @Override
    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = getTransaction(id);

        Budget budget = getBudgetIfPresent(request.getBudgetId());
        Account account = getAccount(request.getAccountId());

        ensureSameOwner(budget, account);

        transaction.setOccurredAt(request.getOccurredAt());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(normalizeDescription(request.getDescription()));
        transaction.setType(request.getType());
        transaction.setBudget(budget);
        transaction.setAccount(account);

        Transaction saved = transactionRepository.save(transaction);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse patch(Long id, TransactionUpdateRequest request) {
        Transaction transaction = getTransaction(id);

        Budget budget = request.getBudgetId() != null
                ? getBudget(request.getBudgetId())
                : transaction.getBudget();
        Account account = request.getAccountId() != null
                ? getAccount(request.getAccountId())
                : transaction.getAccount();

        ensureSameOwner(budget, account);

        if (request.getOccurredAt() != null) {
            transaction.setOccurredAt(request.getOccurredAt());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(normalizeDescription(request.getDescription()));
        }
        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        if (request.getBudgetId() != null) {
            transaction.setBudget(budget);
        }
        if (request.getAccountId() != null) {
            transaction.setAccount(account);
        }

        Transaction saved = transactionRepository.save(transaction);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return transactionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE_PREFIX + id);
        }
        transactionRepository.deleteById(id);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
    }

    private TransactionSearchResult loadAndIndexSearchResult(
            TransactionSearchCacheKey cacheKey,
            TransactionSearchQueryMode queryMode,
            String budgetName,
            String accountName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable repositoryPageable,
            Pageable responsePageable,
            String searchLogContext) {
        log.info("Transaction search cache MISS [{}]", searchLogContext);
        log.info("Transaction search loading from DATABASE [{}]", searchLogContext);
        Page<Transaction> transactions = switch (queryMode) {
            case NATIVE -> transactionRepository.searchByNestedFiltersNative(
                    budgetName,
                    accountName,
                    minAmount,
                    maxAmount,
                    startDateTime,
                    endDateTime,
                    repositoryPageable);
            case JPQL -> transactionRepository.searchByNestedFiltersJpql(
                    budgetName,
                    accountName,
                    minAmount,
                    maxAmount,
                    startDateTime,
                    endDateTime,
                    repositoryPageable);
        };

        Page<TransactionResponse> responsePage = new PageImpl<>(
                transactions.getContent().stream()
                        .map(transaction -> transactionMapper.toResponse(transaction, true, true))
                        .toList(),
                responsePageable,
                transactions.getTotalElements());
        transactionSearchIndex.put(cacheKey, responsePage);
        log.info("Transaction search result cached [{}]", searchLogContext);
        return new TransactionSearchResult(responsePage, TransactionSearchSource.DATABASE);
    }

    private void validateSearchFilters(
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {
        if (minAmount != null && maxAmount != null && maxAmount.compareTo(minAmount) < 0) {
            throw new BadRequestException("maxAmount must be greater than or equal to minAmount");
        }

        if (startDateTime != null && endDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw new BadRequestException("endDateTime must be greater than or equal to startDateTime");
        }
    }

    private String normalizeTextFilterForCache(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeTextFilterForRepository(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        return normalized.isBlank() ? "" : normalized;
    }

    private Pageable normalizePageableForQuery(TransactionSearchQueryMode queryMode, Pageable pageable) {
        Sort normalizedSort = pageable.getSort().isSorted()
                ? normalizeSort(queryMode, pageable.getSort())
                : defaultSort(queryMode);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }

    private Sort normalizeSort(TransactionSearchQueryMode queryMode, Sort sort) {
        List<Sort.Order> normalizedOrders = sort.stream()
                .map(order -> normalizeSortOrder(queryMode, order))
                .toList();
        return Sort.by(normalizedOrders);
    }

    private Sort defaultSort(TransactionSearchQueryMode queryMode) {
        return queryMode == TransactionSearchQueryMode.NATIVE
                ? Sort.by(Sort.Direction.DESC, "occurred_at")
                : Sort.by(Sort.Direction.DESC, "occurredAt");
    }

    private Sort.Order normalizeSortOrder(TransactionSearchQueryMode queryMode, Sort.Order order) {
        String normalizedProperty = queryMode == TransactionSearchQueryMode.NATIVE
                ? mapNativeSortProperty(order.getProperty())
                : mapJpqlSortProperty(order.getProperty());
        Sort.Order normalizedOrder = new Sort.Order(order.getDirection(), normalizedProperty);
        return order.isIgnoreCase() ? normalizedOrder.ignoreCase() : normalizedOrder;
    }

    private String mapJpqlSortProperty(String property) {
        return switch (property) {
            case "id" -> "id";
            case "occurredAt", "occurred_at" -> "occurredAt";
            case "amount" -> "amount";
            case "description" -> "description";
            case "type" -> "type";
            default -> throw new BadRequestException("Unsupported sort property for JPQL query: " + property);
        };
    }

    private String mapNativeSortProperty(String property) {
        return switch (property) {
            case "id" -> "id";
            case "occurredAt", "occurred_at" -> "occurred_at";
            case "amount" -> "amount";
            case "description" -> "description";
            case "type" -> "type";
            case "budgetId", "budget_id" -> "budget_id";
            case "accountId", "account_id" -> "account_id";
            default -> throw new BadRequestException("Unsupported sort property for native query: " + property);
        };
    }

    private String buildSearchLogContext(
            TransactionSearchQueryMode queryMode,
            String budgetName,
            String accountName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable) {
        return "queryMode=" + queryMode
                + ", budgetName=" + valueOrDash(budgetName)
                + ", accountName=" + valueOrDash(accountName)
                + ", minAmount=" + valueOrDash(minAmount)
                + ", maxAmount=" + valueOrDash(maxAmount)
                + ", startDateTime=" + valueOrDash(startDateTime)
                + ", endDateTime=" + valueOrDash(endDateTime)
                + ", page=" + pageable.getPageNumber()
                + ", size=" + pageable.getPageSize()
                + ", sort=" + pageable.getSort();
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : value.toString();
    }

    private Transaction createTransactionEntity(TransactionRequest request) {
        Budget budget = Optional.ofNullable(request.getBudgetId())
                .map(this::getBudget)
                .orElse(null);
        Account account = getAccount(request.getAccountId());

        ensureSameOwner(budget, account);

        Transaction transaction = transactionMapper.fromRequest(request, budget, account);
        transaction.setDescription(normalizeDescription(request.getDescription()));
        Transaction saved = transactionRepository.save(transaction);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return saved;
    }

    private List<TransactionResponse> createBulkInternal(List<TransactionRequest> requests) {
        List<TransactionRequest> bulkRequests = Optional.ofNullable(requests)
                .filter(items -> !items.isEmpty())
                .orElseThrow(() -> new BadRequestException(BULK_REQUEST_EMPTY_MESSAGE));

        return bulkRequests.stream()
                .map(this::createTransactionEntity)
                .map(transaction -> transactionMapper.toResponse(transaction, true, true))
                .toList();
    }

    private Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));
    }

    private Budget getBudgetIfPresent(Long budgetId) {
        if (budgetId == null) {
            return null;
        }
        return getBudget(budgetId);
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }

    private void ensureSameOwner(Budget budget, Account account) {
        if (account.getUser() == null) {
            throw new ConflictException("Account must be owned by a user");
        }

        if (budget == null) {
            return;
        }

        if (budget.getUser() == null) {
            throw new ConflictException("Budget must be owned by a user");
        }

        if (!budget.getUser().getId().equals(account.getUser().getId())) {
            throw new ConflictException("Budget owner and account owner must match");
        }
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> transactionMapper.toResponse(transaction, true, true))
                .toList();
    }

    private Transaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE_PREFIX + id));
    }

    private String normalizeDescription(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Description must not be blank");
        }
        return normalized;
    }
}
