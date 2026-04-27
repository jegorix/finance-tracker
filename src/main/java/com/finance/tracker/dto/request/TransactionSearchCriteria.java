package com.finance.tracker.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionSearchCriteria(
        Long userId,
        TransactionSearchQueryMode queryMode,
        String budgetName,
        String accountName,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime) {
}
