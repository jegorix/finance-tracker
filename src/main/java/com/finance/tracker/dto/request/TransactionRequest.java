package com.finance.tracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class TransactionRequest {

    @NotNull
    private BigDecimal amount;

    @NotNull
    private LocalDate date;

    @NotBlank
    private String accountName;

    @NotBlank
    private String ownerEmail;

    @NotBlank
    private String ownerName;

    @NotBlank
    private String categoryName;

    private List<String> tagNames;
}
