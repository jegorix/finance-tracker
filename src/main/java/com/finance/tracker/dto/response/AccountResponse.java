package com.finance.tracker.dto.response;

import com.finance.tracker.domain.AccountType;

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
@Schema(description = "Account response DTO")
public class AccountResponse {

    private Long id;
    private String name;
    private AccountType type;
    private BigDecimal balance;
}
