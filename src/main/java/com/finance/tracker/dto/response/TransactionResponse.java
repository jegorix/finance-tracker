package com.finance.tracker.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.finance.tracker.domain.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private LocalDateTime occurredAt;
    private BigDecimal amount;
    private String description;
    private TransactionType type;
    private Long budgetId;
    private String budgetName;
    private Long accountId;
    private String accountName;
}
