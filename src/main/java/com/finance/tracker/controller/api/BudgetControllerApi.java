package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.BudgetRequest;
import com.finance.tracker.dto.request.BudgetUpdateRequest;
import com.finance.tracker.dto.response.BudgetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Budgets", description = "Operations with budgets")
public interface BudgetControllerApi {

    @Operation(summary = "Get budget by id")
    ResponseEntity<BudgetResponse> getById(@Positive Long id);

    @Operation(summary = "Get all budgets")
    ResponseEntity<List<BudgetResponse>> getAll();

    @Operation(summary = "Create a budget")
    ResponseEntity<BudgetResponse> create(@Valid BudgetRequest request);

    @Operation(summary = "Update a budget")
    ResponseEntity<BudgetResponse> update(@Positive Long id, @Valid BudgetRequest request);

    @Operation(summary = "Patch a budget")
    ResponseEntity<BudgetResponse> patch(@Positive Long id, @Valid BudgetUpdateRequest request);

    @Operation(summary = "Delete a budget")
    ResponseEntity<Void> delete(@Positive Long id);
}
