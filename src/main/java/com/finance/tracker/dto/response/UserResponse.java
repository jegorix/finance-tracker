package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "User response DTO")
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private List<Long> accountIds;
    private List<Long> budgetIds;
}
