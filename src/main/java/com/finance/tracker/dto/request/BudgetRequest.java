package com.finance.tracker.dto.request;

import java.util.List;

import jakarta.validation.constraints.Min;
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
public class BudgetRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String name;

    @NotNull
    @Min(value = 1)
    private Double limitAmount;

    @NotNull
    @Min(value = 0)
    private Double spent;

    private List<Long> categoryIds;
}
