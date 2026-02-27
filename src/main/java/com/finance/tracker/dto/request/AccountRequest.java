package com.finance.tracker.dto.request;

import com.finance.tracker.domain.AccountType;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String name;

    @NotNull
    private AccountType type;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal balance;
}
