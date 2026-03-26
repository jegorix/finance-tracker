package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndBudgetsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Users", description = "Operations with finance tracker users")
public interface UserControllerApi {

    @Operation(summary = "Get user by id")
    ResponseEntity<UserResponse> getById(@Positive Long id);

    @Operation(summary = "Get all users")
    ResponseEntity<List<UserResponse>> getAll();

    @Operation(summary = "Create a user")
    ResponseEntity<UserResponse> create(@Valid UserRequest request);

    @Operation(summary = "Update a user")
    ResponseEntity<UserResponse> update(@Positive Long id, @Valid UserUpdateRequest request);

    @Operation(summary = "Delete a user")
    ResponseEntity<Void> delete(@Positive Long id);

    @Operation(summary = "Create a user together with accounts and budgets")
    ResponseEntity<UserResponse> createWithAccountsAndBudgets(
            @Valid UserWithAccountsAndBudgetsCreateRequest request,
            @Parameter(description = "Execute operation inside a transaction") boolean transactional);
}
