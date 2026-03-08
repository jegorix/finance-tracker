package com.finance.tracker.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.finance.tracker.domain.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPatchRequest {

    @PastOrPresent
    private LocalDateTime occurredAt;

    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    private TransactionType type;

    @Positive
    private Long budgetId;

    @Positive
    private Long accountId;
}
