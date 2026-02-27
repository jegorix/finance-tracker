package com.finance.tracker.dto.request;

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
public class UserWithAccountsAndTransactionsCreateRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @Email
    private String email;

    @NotEmpty
    @Valid
    private List<AccountRequest> accounts;

    @NotEmpty
    @Valid
    private List<TransactionRequest> transactions;

    private boolean failAfterAccounts;
}
