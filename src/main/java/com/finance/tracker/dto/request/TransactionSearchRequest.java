package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Query parameters for transaction search with pagination and sorting")
public class TransactionSearchRequest {

    @Schema(description = "Part of a budget name", example = "Food")
    @Size(max = 50)
    private String budgetName;

    @Schema(description = "Part of an account name", example = "Main card")
    @Size(max = 50)
    private String accountName;

    @Schema(description = "Minimal transaction amount", example = "10.00")
    @DecimalMin(value = "0.00")
    private BigDecimal minAmount;

    @Schema(description = "Maximum transaction amount", example = "500.00")
    @DecimalMin(value = "0.00")
    private BigDecimal maxAmount;

    @Schema(description = "Start of the date and time range", example = "2026-03-01T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateTime;

    @Schema(description = "End of the date and time range", example = "2026-03-31T23:59:59")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDateTime;

    @Schema(description = "Search implementation", example = "JPQL")
    @NotNull
    private TransactionSearchQueryMode queryMode = TransactionSearchQueryMode.JPQL;

    @Schema(description = "Zero-based page number", example = "0")
    @NotNull
    @PositiveOrZero
    private Integer page = 0;

    @Schema(description = "Page size", example = "5")
    @NotNull
    @Positive
    private Integer size = 5;

    @Schema(description = "Sort field", example = "occurredAt")
    @NotBlank
    @Size(max = 50)
    private String sortBy = "occurredAt";

    @Schema(description = "Sort direction flag", example = "false")
    private boolean ascending;
}
