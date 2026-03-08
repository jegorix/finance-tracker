package com.finance.tracker.dto.response;

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
public class BudgetResponse {

    private Long id;
    private String name;
    private BigDecimal limitAmount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long userId;
    private List<Long> categoryIds;
    private List<Long> transactionIds;
}
