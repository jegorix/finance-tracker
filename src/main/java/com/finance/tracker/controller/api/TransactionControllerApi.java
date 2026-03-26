package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionSearchRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.dto.response.TransactionSearchPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Transactions", description = "Operations with transactions and search")
public interface TransactionControllerApi {

    @Operation(summary = "Get transaction by id")
    ResponseEntity<TransactionResponse> getById(@Positive Long id);

    @Operation(summary = "Get all transactions or filter them by date range")
    ResponseEntity<List<TransactionResponse>> getAll(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            boolean withEntityGraph);

    @Operation(summary = "Search transactions with filters, pagination and sorting")
    ResponseEntity<TransactionSearchPageResponse> search(@Valid TransactionSearchRequest request);

    @Operation(summary = "Create a transaction")
    ResponseEntity<TransactionResponse> create(@Valid TransactionRequest request);

    @Operation(summary = "Create transactions in bulk")
    ResponseEntity<List<TransactionResponse>> createBulk(
            @Valid List<@Valid TransactionRequest> requests,
            boolean transactional);

    @Operation(summary = "Update a transaction")
    ResponseEntity<TransactionResponse> update(@Positive Long id, @Valid TransactionRequest request);

    @Operation(summary = "Patch a transaction")
    ResponseEntity<TransactionResponse> patch(@Positive Long id, @Valid TransactionUpdateRequest request);

    @Operation(summary = "Delete a transaction")
    ResponseEntity<Void> delete(@Positive Long id);
}
