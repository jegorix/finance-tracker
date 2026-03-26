package com.finance.tracker.dto.request;

import com.finance.tracker.domain.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for account creation or full update")
public class AccountRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    private AccountType type;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal balance;

    @NotNull
    @Positive
    private Long userId;
}
