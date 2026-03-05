package com.finance.tracker.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.finance.tracker.domain.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TransactionRequest {

    @NotNull
    @PastOrPresent
    private LocalDateTime occurredAt;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    private TransactionType type;

    @NotNull
    @Positive
    private Long budgetId;

    @NotNull
    @Positive
    private Long accountId;

    @NotNull
    @Positive
    private Long userId;
}
