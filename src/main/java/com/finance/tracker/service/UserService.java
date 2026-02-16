package com.finance.tracker.service;

import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserWithAccountsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest user);

    void deleteUser(Long id);

    UserResponse createUserWithAccountsTx(UserWithAccountsCreateRequest request);

    UserResponse createUserWithAccountsNoTx(UserWithAccountsCreateRequest request);
}
