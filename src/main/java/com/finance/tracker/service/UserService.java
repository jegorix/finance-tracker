package com.finance.tracker.service;

import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndBudgetsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse findById(Long id);

    List<UserResponse> findAll();

    UserResponse create(UserRequest request);

    UserResponse update(Long id, UserRequest user);

    void delete(Long id);

    UserResponse createWithAccountsAndBudgetsTx(UserWithAccountsAndBudgetsCreateRequest request);

    UserResponse createWithAccountsAndBudgetsNoTx(UserWithAccountsAndBudgetsCreateRequest request);
}
