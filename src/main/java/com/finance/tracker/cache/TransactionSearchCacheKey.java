package com.finance.tracker.cache;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.finance.tracker.dto.request.TransactionSearchQueryMode;

public final class TransactionSearchCacheKey {

    private final TransactionSearchQueryMode queryMode;
    private final String budgetName;
    private final String accountName;
    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final int pageNumber;
    private final int pageSize;
    private final String sort;

    public TransactionSearchCacheKey(
            TransactionSearchQueryMode queryMode,
            String budgetName,
            String accountName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int pageNumber,
            int pageSize,
            String sort) {
        this.queryMode = queryMode;
        this.budgetName = budgetName;
        this.accountName = accountName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sort = sort;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransactionSearchCacheKey that)) {
            return false;
        }
        return pageNumber == that.pageNumber
                && pageSize == that.pageSize
                && queryMode == that.queryMode
                && Objects.equals(budgetName, that.budgetName)
                && Objects.equals(accountName, that.accountName)
                && Objects.equals(minAmount, that.minAmount)
                && Objects.equals(maxAmount, that.maxAmount)
                && Objects.equals(startDateTime, that.startDateTime)
                && Objects.equals(endDateTime, that.endDateTime)
                && Objects.equals(sort, that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                queryMode,
                budgetName,
                accountName,
                minAmount,
                maxAmount,
                startDateTime,
                endDateTime,
                pageNumber,
                pageSize,
                sort);
    }
}
