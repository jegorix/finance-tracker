package com.finance.tracker.service;

import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndTransactionsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest user);

    void deleteUser(Long id);

    UserResponse createUserWithAccountsAndTransactionsTx(UserWithAccountsAndTransactionsCreateRequest request);

    UserResponse createUserWithAccountsAndTransactionsNoTx(UserWithAccountsAndTransactionsCreateRequest request);
}
