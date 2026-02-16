package com.finance.tracker.controller;

import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import com.finance.tracker.service.BudgetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
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
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService service;

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getBudgetById(id));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets(
            @RequestParam(required = false, defaultValue = "false") boolean withTransactions) {
        List<BudgetResponse> budgets = withTransactions
                ? service.getAllBudgetsWithTransactions()
                : service.getAllBudgets();
        return ResponseEntity.ok(budgets);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(service.createBudget(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(service.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        service.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
