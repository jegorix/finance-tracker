package com.finance.tracker.service;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {
    AccountResponse getAccountById(Long id);

    List<AccountResponse> getAllAccounts();

    AccountResponse createAccount(AccountRequest request);

    AccountResponse updateAccount(Long id, AccountRequest request);

    void deleteAccount(Long id);
}
