package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.service.AccountService;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found " + id));
        return accountMapper.toResponse(account);
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return toResponses(accountRepository.findAll());
    }

    @Override
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        Account account = accountMapper.fromRequest(request);
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(Long id, AccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found " + id));
        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        if (request.getBalance() != null) {
            account.setBalance(request.getBalance());
        }
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found " + id);
        }
        accountRepository.deleteById(id);
    }

    private List<AccountResponse> toResponses(List<Account> accounts) {
        return accounts.stream().map(accountMapper::toResponse).toList();
    }
}
