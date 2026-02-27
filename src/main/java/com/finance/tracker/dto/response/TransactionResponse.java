package com.finance.tracker.dto.response;

import java.time.LocalDate;

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
    private LocalDate date;
    private Double amount;
    private String description;
    private Long budgetId;
    private String budgetName;
    private Long userId;
    private String username;
}
