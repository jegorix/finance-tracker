package com.finance.tracker.service;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {
    AccountResponse findById(Long id);

    List<AccountResponse> findAll();

    AccountResponse create(AccountRequest request);

    AccountResponse update(Long id, AccountRequest request);

    void delete(Long id);
}
