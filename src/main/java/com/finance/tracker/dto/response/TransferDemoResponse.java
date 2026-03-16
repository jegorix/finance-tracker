package com.finance.tracker.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferDemoResponse {

    private Long fromAccountId;
    private BigDecimal fromAccountBalance;
    private Long toAccountId;
    private BigDecimal toAccountBalance;
    private BigDecimal transferredAmount;
}
