package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transfer demo response showing balances after transfer")
public class TransferDemoResponse {

    private Long fromAccountId;
    private BigDecimal fromAccountBalance;
    private Long toAccountId;
    private BigDecimal toAccountBalance;
    private BigDecimal transferredAmount;
}
