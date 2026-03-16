package com.finance.tracker.service;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.TransferDemoRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.dto.response.TransferDemoResponse;

import java.util.List;

public interface AccountService {
    AccountResponse findById(Long id);

    List<AccountResponse> findAll();

    AccountResponse create(AccountRequest request);

    AccountResponse update(Long id, AccountRequest request);

    TransferDemoResponse transferTx(TransferDemoRequest request);

    TransferDemoResponse transferNoTx(TransferDemoRequest request);

    void delete(Long id);
}
