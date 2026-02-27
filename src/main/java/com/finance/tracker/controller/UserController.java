package com.finance.tracker.controller;

import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndTransactionsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = service.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest userDetails) {
        return ResponseEntity.ok(service.updateUser(id, userDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/with-accounts-and-transactions")
    public ResponseEntity<UserResponse> createWithAccountsAndTransactions(
            @Valid @RequestBody UserWithAccountsAndTransactionsCreateRequest request,
            @RequestParam(defaultValue = "true") boolean transactional) {
        UserResponse response = transactional
                ? service.createUserWithAccountsAndTransactionsTx(request)
                : service.createUserWithAccountsAndTransactionsNoTx(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
