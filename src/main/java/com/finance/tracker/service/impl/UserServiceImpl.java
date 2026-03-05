package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndBudgetsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    @Override
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> findAll() {
        return toResponses(userRepository.findAll());
    }

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        User user = userMapper.fromRequest(request);
        user.setUsername(request.getUsername().trim());
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + id));

        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail());

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserResponse createWithAccountsAndBudgetsNoTx(UserWithAccountsAndBudgetsCreateRequest request) {
        return createWithAccountsAndBudgetsInternal(request);
    }

    @Override
    @Transactional
    public UserResponse createWithAccountsAndBudgetsTx(UserWithAccountsAndBudgetsCreateRequest request) {
        return createWithAccountsAndBudgetsInternal(request);
    }

    private UserResponse createWithAccountsAndBudgetsInternal(UserWithAccountsAndBudgetsCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);

        for (AccountRequest accountRequest : request.getAccounts()) {
            Account account = accountMapper.fromRequest(accountRequest);
            account.setName(accountRequest.getName().trim());
            account.setUser(savedUser);
            savedUser.getAccounts().add(account);
            accountRepository.save(account);
        }

        if (request.isFailAfterAccounts()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Forced error right after all accounts were saved");
        }

        createBudgetsForRequest(savedUser, request.getBudgets());
        return userMapper.toResponse(savedUser);
    }

    private void createBudgetsForRequest(User user, List<BudgetRequest> budgets) {
        for (BudgetRequest budgetRequest : budgets) {
            Budget budget = new Budget();
            budget.setName(budgetRequest.getName().trim());
            budget.setLimitAmount(budgetRequest.getLimitAmount());
            budget.setPeriodStart(budgetRequest.getPeriodStart());
            budget.setPeriodEnd(budgetRequest.getPeriodEnd());
            budget.setUser(user);

            Budget savedBudget = budgetRepository.save(budget);
            user.getBudgets().add(savedBudget);
        }
    }

    private List<UserResponse> toResponses(List<User> users) {
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
