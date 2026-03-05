package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Override
    public AccountResponse findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found " + id));
        return accountMapper.toResponse(account);
    }

    @Override
    public List<AccountResponse> findAll() {
        return toResponses(accountRepository.findAll());
    }

    @Override
    @Transactional
    public AccountResponse create(AccountRequest request) {
        User user = getUser(request.getUserId());
        Account account = accountMapper.fromRequest(request);
        account.setName(request.getName().trim());
        account.setUser(user);

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AccountResponse update(Long id, AccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found " + id));
        User user = getUser(request.getUserId());

        account.setName(request.getName().trim());
        account.setType(request.getType());
        account.setBalance(request.getBalance());
        account.setUser(user);

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found " + id);
        }
        accountRepository.deleteById(id);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + userId));
    }

    private List<AccountResponse> toResponses(List<Account> accounts) {
        return accounts.stream().map(accountMapper::toResponse).toList();
    }
}
