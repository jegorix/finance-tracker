package com.finance.tracker.service.impl;

import com.finance.tracker.auth.AuthContext;
import com.finance.tracker.auth.AuthSessionService;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndBudgetsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.LoggingException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.mapper.UserMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.UserService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final AuthSessionService authSessionService;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            AccountRepository accountRepository,
            BudgetRepository budgetRepository,
            UserMapper userMapper,
            AccountMapper accountMapper,
            AuthSessionService authSessionService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.budgetRepository = budgetRepository;
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
        this.authSessionService = authSessionService;
    }

    @Override
    public UserResponse findById(Long id) {
        Long currentUserId = currentUserId();
        if (currentUserId != null && !currentUserId.equals(id)) {
            throw new ResourceNotFoundException("User not found " + id);
        }
        return userMapper.toResponse(getUser(currentUserId != null ? currentUserId : id));
    }

    @Override
    public List<UserResponse> findAll() {
        Long currentUserId = currentUserId();
        return currentUserId == null
                ? toResponses(userRepository.findAll())
                : List.of(userMapper.toResponse(getUser(currentUserId)));
    }

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail(), true);
        ensureUniqueCredentials(username, email, null);

        User user = userMapper.fromRequest(request);
        user.setUsername(username);
        user.setEmail(email);
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getCurrentUser(id);
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail(), false);
        ensureUniqueCredentials(username, email, user.getId());

        user.setUsername(username);
        if (email != null) {
            user.setEmail(email);
        }

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (currentUserId() == null) {
            if (!userRepository.existsById(id)) {
                throw new ResourceNotFoundException("User not found " + id);
            }
            userRepository.deleteById(id);
            return;
        }

        User user = getCurrentUser(id);
        if (!userRepository.existsById(user.getId())) {
            throw new ResourceNotFoundException("User not found " + id);
        }
        userRepository.deleteById(user.getId());
        if (authSessionService != null) {
            authSessionService.invalidateAllForUser(user.getId());
        }
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
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail(), true);
        ensureUniqueCredentials(username, email, null);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        User savedUser = userRepository.save(user);

        for (AccountRequest accountRequest : request.getAccounts()) {
            Account account = accountMapper.fromRequest(accountRequest);
            account.setName(accountRequest.getName().trim());
            account.setUser(savedUser);
            savedUser.getAccounts().add(account);
            accountRepository.save(account);
        }

        if (request.isFailAfterAccounts()) {
            throw new LoggingException("Forced error right after all accounts were saved");
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

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + id));
    }

    private User getCurrentUser(Long requestedUserId) {
        Long currentUserId = currentUserId();
        if (currentUserId == null) {
            return getUser(requestedUserId);
        }
        if (!currentUserId.equals(requestedUserId)) {
            throw new ResourceNotFoundException("User not found " + requestedUserId);
        }
        return getUser(currentUserId);
    }

    public UserServiceImpl(
            UserRepository userRepository,
            AccountRepository accountRepository,
            BudgetRepository budgetRepository,
            UserMapper userMapper,
            AccountMapper accountMapper) {
        this(userRepository, accountRepository, budgetRepository, userMapper, accountMapper, null);
    }

    private Long currentUserId() {
        return AuthContext.getCurrentUserId();
    }

    private void ensureUniqueCredentials(String username, String email, Long currentUserId) {
        boolean usernameExists = currentUserId == null
                ? userRepository.existsByUsernameIgnoreCase(username)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, currentUserId);
        if (usernameExists) {
            throw new DuplicateResourceException("User with username '" + username + "' already exists");
        }

        if (email == null) {
            return;
        }

        boolean emailExists = currentUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentUserId);
        if (emailExists) {
            throw new DuplicateResourceException("User with email '" + email + "' already exists");
        }
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? null : username.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("Username must not be blank");
        }
        return normalized;
    }

    private String normalizeEmail(String email, boolean required) {
        if (email == null) {
            if (required) {
                throw new BadRequestException("Email must not be blank");
            }
            return null;
        }

        String normalized = email.trim();
        if (normalized.isBlank()) {
            throw new BadRequestException("Email must not be blank");
        }
        return normalized;
    }
}
