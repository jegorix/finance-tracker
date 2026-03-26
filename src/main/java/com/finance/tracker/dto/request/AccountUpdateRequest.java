package com.finance.tracker.dto.request;

import com.finance.tracker.domain.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating an account")
public class AccountUpdateRequest {

    @Size(min = 3, max = 50)
    private String name;

    private AccountType type;

    @DecimalMin(value = "0.00")
    private BigDecimal balance;

    @Positive
    private Long userId;
}
