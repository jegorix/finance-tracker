package com.finance.tracker.mapper;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.domain.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }

        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setBalance(account.getBalance());

        return response;
    }

    public Account fromRequest(AccountRequest request) {
        if (request == null) {
            return null;
        }

        Account account = new Account();
        account.setName(request.getName());
        account.setType(request.getType());
        account.setBalance(request.getBalance());

        return account;
    }

    public AccountRequest toRequest(Account account) {
        if (account == null) {
            return null;
        }

        AccountRequest request = new AccountRequest();
        request.setName(account.getName());
        request.setType(account.getType());
        request.setBalance(account.getBalance());

        return request;
    }
}
