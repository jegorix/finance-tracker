package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

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
@Schema(description = "Request body for category creation or full update")
public class CategoryRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    @Positive
    private Long userId;

    private List<@Positive Long> budgetIds;
}
