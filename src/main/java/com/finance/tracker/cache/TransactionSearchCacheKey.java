package com.finance.tracker.cache;

import com.finance.tracker.dto.request.TransactionSearchCriteria;

public record TransactionSearchCacheKey(
        Long userId,
        TransactionSearchCriteria criteria,
        int pageNumber,
        int pageSize,
        String sort) {
}
