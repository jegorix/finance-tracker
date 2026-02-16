package com.finance.tracker.dto.response;

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
    private Double limitAmount;
    private Double spent;
    private List<Long> categoryIds;
    private List<Long> transactionIds;
}
