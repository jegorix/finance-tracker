package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for transfer demo used to show transaction behavior")
public class TransferDemoRequest {

    @NotNull
    @Positive
    private Long fromAccountId;

    @NotNull
    @Positive
    private Long toAccountId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private boolean failAfterDebit;
}
