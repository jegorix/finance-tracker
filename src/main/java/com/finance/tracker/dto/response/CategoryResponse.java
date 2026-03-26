package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category response DTO")
public class CategoryResponse {

    private Long id;
    private String name;
    private Long userId;
    private List<Long> budgetIds;
}
