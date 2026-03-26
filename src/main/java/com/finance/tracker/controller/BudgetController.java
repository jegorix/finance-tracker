package com.finance.tracker.controller;

import com.finance.tracker.controller.api.BudgetControllerApi;
import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.BudgetUpdateRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.service.BudgetService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/budgets")
public class BudgetController implements BudgetControllerApi {

    private final BudgetService budgetService;

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<BudgetResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(budgetService.findById(id));
    }

    @GetMapping
    @Override
    public ResponseEntity<List<BudgetResponse>> getAll() {
        return ResponseEntity.ok(budgetService.findAll());
    }

    @PostMapping
    @Override
    public ResponseEntity<BudgetResponse> create(@RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<BudgetResponse> update(
            @PathVariable("id") Long id,
            @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(id, request));
    }

    @PatchMapping("/{id}")
    @Override
    public ResponseEntity<BudgetResponse> patch(
            @PathVariable("id") Long id,
            @RequestBody BudgetUpdateRequest request) {
        return ResponseEntity.ok(budgetService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
