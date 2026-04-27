package com.finance.tracker.service.impl;

import java.math.BigDecimal;
import com.finance.tracker.auth.AuthContext;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.cache.TransactionSearchCacheKey;
import com.finance.tracker.cache.TransactionSearchIndex;
import com.finance.tracker.cache.TransactionSearchIndexInvalidator;
import com.finance.tracker.dto.request.TransactionSearchCriteria;
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
    private static final String SORT_FIELD_ID = "id";
    private static final String SORT_PROPERTY_OCCURRED_AT = "occurredAt";
    private static final String SORT_COLUMN_OCCURRED_AT = "occurred_at";
    private static final String SORT_FIELD_AMOUNT = "amount";
    private static final String SORT_FIELD_DESCRIPTION = "description";
    private static final String SORT_FIELD_TYPE = "type";
    private static final String SORT_PROPERTY_BUDGET_ID = "budgetId";
    private static final String SORT_COLUMN_BUDGET_ID = "budget_id";
    private static final String SORT_PROPERTY_ACCOUNT_ID = "accountId";
    private static final String SORT_COLUMN_ACCOUNT_ID = "account_id";

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
        Long currentUserId = currentUserId();
        List<Transaction> transactions;
        if (currentUserId == null) {
            transactions = withEntityGraph
                    ? transactionRepository.findAllTransactionsWithEntityGraph()
                    : transactionRepository.findAllTransactions();
        } else {
            transactions = withEntityGraph
                    ? transactionRepository.findAllTransactionsWithEntityGraph(currentUserId)
                    : transactionRepository.findAllTransactions(currentUserId);
        }
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

        Long currentUserId = currentUserId();
        List<Transaction> transactions = currentUserId == null
                ? transactionRepository.findByOccurredAtBetween(startDateTime, endDateTime)
                : transactionRepository.findByOccurredAtBetweenAndAccountUserId(startDateTime, endDateTime, currentUserId);
        return toResponses(transactions);
    }

    @Override
    public TransactionSearchResult search(TransactionSearchCriteria criteria, Pageable pageable) {
        Long targetUserId = criteria != null && criteria.userId() != null ? criteria.userId() : currentUserId();
        PreparedTransactionSearch preparedSearch = prepareSearch(criteria, pageable, targetUserId);

        return transactionSearchIndex.find(preparedSearch.cacheKey())
                .map(page -> {
                    log.info("Transaction search cache HIT [{}]", preparedSearch.searchLogContext());
                    return new TransactionSearchResult(page, TransactionSearchSource.CACHE);
                })
                .orElseGet(() -> loadAndIndexSearchResult(preparedSearch));
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
        Long currentUserId = currentUserId();
        boolean exists = currentUserId == null
                ? transactionRepository.existsById(id)
                : transactionRepository.existsByIdAndAccountUserId(id, currentUserId);
        if (!exists) {
            throw new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE_PREFIX + id);
        }
        transactionRepository.deleteById(id);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
    }

    private PreparedTransactionSearch prepareSearch(TransactionSearchCriteria criteria, Pageable pageable, Long userId) {
        TransactionSearchCriteria normalizedCriteria = normalizeCriteria(criteria);
        validateSearchFilters(
                normalizedCriteria.minAmount(),
                normalizedCriteria.maxAmount(),
                normalizedCriteria.startDateTime(),
                normalizedCriteria.endDateTime());

        Pageable repositoryPageable = normalizePageableForQuery(normalizedCriteria.queryMode(), pageable);
        TransactionSearchCriteria cacheCriteria = normalizeCriteriaForCache(normalizedCriteria);
        TransactionSearchCriteria repositoryCriteria = normalizeCriteriaForRepository(normalizedCriteria);
        TransactionSearchCacheKey cacheKey = new TransactionSearchCacheKey(
                userId,
                cacheCriteria,
                repositoryPageable.getPageNumber(),
                repositoryPageable.getPageSize(),
                repositoryPageable.getSort().toString());
        String searchLogContext = buildSearchLogContext(cacheCriteria, repositoryPageable);
        return new PreparedTransactionSearch(
                userId,
                cacheKey,
                repositoryCriteria,
                repositoryPageable,
                pageable,
                searchLogContext);
    }

    private TransactionSearchCriteria normalizeCriteria(TransactionSearchCriteria criteria) {
        if (criteria == null) {
            return new TransactionSearchCriteria(null, TransactionSearchQueryMode.JPQL, null, null, null, null, null, null);
        }
        return new TransactionSearchCriteria(
                criteria.userId(),
                criteria.queryMode() == null ? TransactionSearchQueryMode.JPQL : criteria.queryMode(),
                criteria.budgetName(),
                criteria.accountName(),
                criteria.minAmount(),
                criteria.maxAmount(),
                criteria.startDateTime(),
                criteria.endDateTime());
    }

    private TransactionSearchCriteria normalizeCriteriaForCache(TransactionSearchCriteria criteria) {
        return new TransactionSearchCriteria(
                criteria.userId(),
                criteria.queryMode(),
                normalizeTextFilterForCache(criteria.budgetName()),
                normalizeTextFilterForCache(criteria.accountName()),
                criteria.minAmount(),
                criteria.maxAmount(),
                criteria.startDateTime(),
                criteria.endDateTime());
    }

    private TransactionSearchCriteria normalizeCriteriaForRepository(TransactionSearchCriteria criteria) {
        return new TransactionSearchCriteria(
                criteria.userId(),
                criteria.queryMode(),
                normalizeTextFilterForRepository(criteria.budgetName()),
                normalizeTextFilterForRepository(criteria.accountName()),
                criteria.minAmount() == null ? SEARCH_MIN_AMOUNT : criteria.minAmount(),
                criteria.maxAmount() == null ? SEARCH_MAX_AMOUNT : criteria.maxAmount(),
                criteria.startDateTime() == null ? SEARCH_START_DATE_TIME : criteria.startDateTime(),
                criteria.endDateTime() == null ? SEARCH_END_DATE_TIME : criteria.endDateTime());
    }

    private TransactionSearchResult loadAndIndexSearchResult(PreparedTransactionSearch preparedSearch) {
        TransactionSearchCriteria repositoryCriteria = preparedSearch.repositoryCriteria();
        log.info("Transaction search cache MISS [{}]", preparedSearch.searchLogContext());
        log.info("Transaction search loading from DATABASE [{}]", preparedSearch.searchLogContext());
        Page<Transaction> transactions = switch (repositoryCriteria.queryMode()) {
            case NATIVE -> preparedSearch.userId() == null
                    ? transactionRepository.searchByNestedFiltersNative(
                            repositoryCriteria.budgetName(),
                            repositoryCriteria.accountName(),
                            repositoryCriteria.minAmount(),
                            repositoryCriteria.maxAmount(),
                            repositoryCriteria.startDateTime(),
                            repositoryCriteria.endDateTime(),
                            preparedSearch.repositoryPageable())
                    : transactionRepository.searchByNestedFiltersNative(
                            preparedSearch.userId(),
                            repositoryCriteria.budgetName(),
                            repositoryCriteria.accountName(),
                            repositoryCriteria.minAmount(),
                            repositoryCriteria.maxAmount(),
                            repositoryCriteria.startDateTime(),
                            repositoryCriteria.endDateTime(),
                            preparedSearch.repositoryPageable());
            case JPQL -> preparedSearch.userId() == null
                    ? transactionRepository.searchByNestedFiltersJpql(
                            repositoryCriteria.budgetName(),
                            repositoryCriteria.accountName(),
                            repositoryCriteria.minAmount(),
                            repositoryCriteria.maxAmount(),
                            repositoryCriteria.startDateTime(),
                            repositoryCriteria.endDateTime(),
                            preparedSearch.repositoryPageable())
                    : transactionRepository.searchByNestedFiltersJpql(
                            preparedSearch.userId(),
                            repositoryCriteria.budgetName(),
                            repositoryCriteria.accountName(),
                            repositoryCriteria.minAmount(),
                            repositoryCriteria.maxAmount(),
                            repositoryCriteria.startDateTime(),
                            repositoryCriteria.endDateTime(),
                            preparedSearch.repositoryPageable());
        };

        Page<TransactionResponse> responsePage = new PageImpl<>(
                transactions.getContent().stream()
                        .map(transaction -> transactionMapper.toResponse(transaction, true, true))
                        .toList(),
                preparedSearch.responsePageable(),
                transactions.getTotalElements());
        transactionSearchIndex.put(preparedSearch.cacheKey(), responsePage);
        log.info("Transaction search result cached [{}]", preparedSearch.searchLogContext());
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
                ? Sort.by(Sort.Direction.DESC, SORT_COLUMN_OCCURRED_AT)
                : Sort.by(Sort.Direction.DESC, SORT_PROPERTY_OCCURRED_AT);
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
            case SORT_FIELD_ID -> SORT_FIELD_ID;
            case SORT_PROPERTY_OCCURRED_AT, SORT_COLUMN_OCCURRED_AT -> SORT_PROPERTY_OCCURRED_AT;
            case SORT_FIELD_AMOUNT -> SORT_FIELD_AMOUNT;
            case SORT_FIELD_DESCRIPTION -> SORT_FIELD_DESCRIPTION;
            case SORT_FIELD_TYPE -> SORT_FIELD_TYPE;
            default -> throw new BadRequestException("Unsupported sort property for JPQL query: " + property);
        };
    }

    private String mapNativeSortProperty(String property) {
        return switch (property) {
            case SORT_FIELD_ID -> SORT_FIELD_ID;
            case SORT_PROPERTY_OCCURRED_AT, SORT_COLUMN_OCCURRED_AT -> SORT_COLUMN_OCCURRED_AT;
            case SORT_FIELD_AMOUNT -> SORT_FIELD_AMOUNT;
            case SORT_FIELD_DESCRIPTION -> SORT_FIELD_DESCRIPTION;
            case SORT_FIELD_TYPE -> SORT_FIELD_TYPE;
            case SORT_PROPERTY_BUDGET_ID, SORT_COLUMN_BUDGET_ID -> SORT_COLUMN_BUDGET_ID;
            case SORT_PROPERTY_ACCOUNT_ID, SORT_COLUMN_ACCOUNT_ID -> SORT_COLUMN_ACCOUNT_ID;
            default -> throw new BadRequestException("Unsupported sort property for native query: " + property);
        };
    }

    private String buildSearchLogContext(TransactionSearchCriteria criteria, Pageable pageable) {
        return "queryMode=" + criteria.queryMode()
                + ", budgetName=" + valueOrDash(criteria.budgetName())
                + ", accountName=" + valueOrDash(criteria.accountName())
                + ", minAmount=" + valueOrDash(criteria.minAmount())
                + ", maxAmount=" + valueOrDash(criteria.maxAmount())
                + ", startDateTime=" + valueOrDash(criteria.startDateTime())
                + ", endDateTime=" + valueOrDash(criteria.endDateTime())
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
        Long currentUserId = currentUserId();
        return (currentUserId == null
                ? budgetRepository.findById(budgetId)
                : budgetRepository.findByIdAndUserId(budgetId, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));
    }

    private Budget getBudgetIfPresent(Long budgetId) {
        if (budgetId == null) {
            return null;
        }
        return getBudget(budgetId);
    }

    private Account getAccount(Long accountId) {
        Long currentUserId = currentUserId();
        return (currentUserId == null
                ? accountRepository.findById(accountId)
                : accountRepository.findByIdAndUserId(accountId, currentUserId))
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
        Long currentUserId = currentUserId();
        return (currentUserId == null
                ? transactionRepository.findById(id)
                : transactionRepository.findByIdAndAccountUserId(id, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE_PREFIX + id));
    }

    private Long currentUserId() {
        return AuthContext.getCurrentUserId();
    }

    private String normalizeDescription(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Description must not be blank");
        }
        return normalized;
    }

    private record PreparedTransactionSearch(
            Long userId,
            TransactionSearchCacheKey cacheKey,
            TransactionSearchCriteria repositoryCriteria,
            Pageable repositoryPageable,
            Pageable responsePageable,
            String searchLogContext) {
    }
}
