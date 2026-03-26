package com.finance.tracker.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.finance.tracker.domain.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body for transaction creation or full update")
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

    @Positive
    private Long budgetId;

    @NotNull
    @Positive
    private Long accountId;

}
