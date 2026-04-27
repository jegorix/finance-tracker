package com.finance.tracker.service.impl;

import com.finance.tracker.auth.AuthContext;
import com.finance.tracker.cache.TransactionSearchIndexInvalidator;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.AccountUpdateRequest;
import com.finance.tracker.dto.request.TransferDemoRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.dto.response.TransferDemoResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.LoggingException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.AccountService;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final TransactionSearchIndexInvalidator transactionSearchIndexInvalidator;

    @Override
    public AccountResponse findById(Long id) {
        return accountMapper.toResponse(getAccount(id));
    }

    @Override
    public List<AccountResponse> findAll() {
        Long currentUserId = currentUserId();
        return toResponses(currentUserId == null
                ? accountRepository.findAll()
                : accountRepository.findAllByUserId(currentUserId));
    }

    @Override
    @Transactional
    public AccountResponse create(AccountRequest request) {
        Long currentUserId = currentUserId();
        if (currentUserId != null) {
            ensureCurrentUser(request.getUserId(), currentUserId);
        }
        User user = getUser(currentUserId != null ? currentUserId : request.getUserId());
        String normalizedName = normalizeName(request.getName(), "Account name must not be blank");
        ensureUniqueAccountName(normalizedName, user.getId(), null);
        Account account = accountMapper.fromRequest(request);
        account.setName(normalizedName);
        account.setUser(user);

        Account saved = accountRepository.save(account);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest request) {
        Account account = getAccount(id);
        Long currentUserId = currentUserId();
        Long targetUserId = request.getUserId() != null ? request.getUserId() : account.getUser().getId();
        User user;
        if (currentUserId != null) {
            ensureCurrentUser(targetUserId, currentUserId);
            user = getUser(currentUserId);
        } else {
            user = request.getUserId() != null ? getUser(targetUserId) : account.getUser();
        }
        String normalizedName = request.getName() != null
                ? normalizeName(request.getName(), "Account name must not be blank")
                : account.getName();
        ensureUniqueAccountName(normalizedName, user.getId(), account.getId());

        if (request.getName() != null) {
            account.setName(normalizedName);
        }
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        if (request.getBalance() != null) {
            account.setBalance(request.getBalance());
        }
        if (request.getUserId() != null) {
            account.setUser(user);
        }

        Account saved = accountRepository.save(account);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferDemoResponse transferTx(TransferDemoRequest request) {
        return transferInternal(request);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TransferDemoResponse transferNoTx(TransferDemoRequest request) {
        return transferInternal(request);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long currentUserId = currentUserId();
        boolean exists = currentUserId == null
                ? accountRepository.existsById(id)
                : accountRepository.existsByIdAndUserId(id, currentUserId);
        if (!exists) {
            throw new ResourceNotFoundException("Account not found " + id);
        }
        accountRepository.deleteById(id);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();
    }

    private TransferDemoResponse transferInternal(TransferDemoRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new BadRequestException("Transfer requires two different accounts");
        }

        Account fromAccount = getAccount(request.getFromAccountId());
        Account toAccount = getAccount(request.getToAccountId());
        BigDecimal amount = request.getAmount();

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds on source account");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        Account savedFromAccount = accountRepository.save(fromAccount);

        if (request.isFailAfterDebit()) {
            throw new LoggingException("Forced error right after money was debited from the source account");
        }

        toAccount.setBalance(toAccount.getBalance().add(amount));
        Account savedToAccount = accountRepository.save(toAccount);
        transactionSearchIndexInvalidator.invalidateAfterCommitOrNow();

        return new TransferDemoResponse(
                savedFromAccount.getId(),
                savedFromAccount.getBalance(),
                savedToAccount.getId(),
                savedToAccount.getBalance(),
                amount);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + userId));
    }

    private Account getAccount(Long accountId) {
        Long currentUserId = currentUserId();
        return (currentUserId == null
                ? accountRepository.findById(accountId)
                : accountRepository.findByIdAndUserId(accountId, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found " + accountId));
    }

    private Long currentUserId() {
        return AuthContext.getCurrentUserId();
    }

    private void ensureCurrentUser(Long requestUserId, Long currentUserId) {
        if (!currentUserId.equals(requestUserId)) {
            throw new ResourceNotFoundException("User not found " + requestUserId);
        }
    }

    private void ensureUniqueAccountName(String name, Long userId, Long currentAccountId) {
        boolean exists = currentAccountId == null
                ? accountRepository.existsByNameIgnoreCaseAndUserId(name, userId)
                : accountRepository.existsByNameIgnoreCaseAndUserIdAndIdNot(name, userId, currentAccountId);
        if (exists) {
            throw new DuplicateResourceException("Account with name '" + name + "' already exists for user " + userId);
        }
    }

    private String normalizeName(String value, String blankMessage) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException(blankMessage);
        }
        return normalized;
    }

    private List<AccountResponse> toResponses(List<Account> accounts) {
        return accounts.stream().map(accountMapper::toResponse).toList();
    }
}
