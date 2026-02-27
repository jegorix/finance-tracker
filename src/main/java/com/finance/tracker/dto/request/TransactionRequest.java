package com.finance.tracker.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @FutureOrPresent
    private LocalDate date;

    @NotNull
    @Positive
    private Double amount;

    @NotBlank
    @Size(min = 3, max = 50)
    private String description;

    @NotNull
    @Positive
    private Long budgetId;

    @Positive
    private Long userId;
}
