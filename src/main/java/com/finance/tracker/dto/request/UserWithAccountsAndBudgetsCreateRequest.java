package com.finance.tracker.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body for transactional demo of user, accounts and budgets creation")
public class UserWithAccountsAndBudgetsCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotEmpty
    @Valid
    private List<AccountRequest> accounts;

    @NotEmpty
    @Valid
    private List<BudgetRequest> budgets;

    private boolean failAfterAccounts;
}
