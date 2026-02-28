package com.finance.tracker.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private LocalDate date;
    private String accountName;
    private String ownerEmail;
    private String categoryName;
    private List<String> tags;
}
