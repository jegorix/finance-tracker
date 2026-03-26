package com.finance.tracker.controller;

import com.finance.tracker.controller.api.UserControllerApi;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.request.UserUpdateRequest;
import com.finance.tracker.dto.request.UserWithAccountsAndBudgetsCreateRequest;
import com.finance.tracker.dto.response.UserResponse;
import com.finance.tracker.service.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerApi {

    private final UserService userService;

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<UserResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping
    @Override
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping
    @Override
    public ResponseEntity<UserResponse> create(@RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<UserResponse> update(
            @PathVariable("id") Long id,
            @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/create-accounts-and-budgets")
    @Override
    public ResponseEntity<UserResponse> createWithAccountsAndBudgets(
            @RequestBody UserWithAccountsAndBudgetsCreateRequest request,
            @RequestParam(defaultValue = "true") boolean transactional) {
        UserResponse response = transactional
                ? userService.createWithAccountsAndBudgetsTx(request)
                : userService.createWithAccountsAndBudgetsNoTx(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
