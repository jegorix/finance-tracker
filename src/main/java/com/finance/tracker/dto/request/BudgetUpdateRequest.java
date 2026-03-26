package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for partially updating a budget")
public class BudgetUpdateRequest {

    @Size(min = 3, max = 50)
    private String name;

    @DecimalMin(value = "0.00")
    private BigDecimal limitAmount;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    @Positive
    private Long userId;

    private List<@Positive Long> categoryIds;
}
