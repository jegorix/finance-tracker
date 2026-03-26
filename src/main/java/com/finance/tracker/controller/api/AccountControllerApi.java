package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.AccountUpdateRequest;
import com.finance.tracker.dto.request.TransferDemoRequest;
import com.finance.tracker.dto.response.AccountResponse;
import com.finance.tracker.dto.response.TransferDemoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Accounts", description = "Operations with user accounts and transfer demo")
public interface AccountControllerApi {

    @Operation(summary = "Get account by id")
    ResponseEntity<AccountResponse> getById(@Positive Long id);

    @Operation(summary = "Get all accounts")
    ResponseEntity<List<AccountResponse>> getAll();

    @Operation(summary = "Create an account")
    ResponseEntity<AccountResponse> create(@Valid AccountRequest request);

    @Operation(summary = "Update an account")
    ResponseEntity<AccountResponse> update(@Positive Long id, @Valid AccountUpdateRequest request);

    @Operation(summary = "Transfer money between accounts")
    ResponseEntity<TransferDemoResponse> transfer(
            @Valid TransferDemoRequest request,
            @Parameter(description = "Execute operation inside a transaction") boolean transactional);

    @Operation(summary = "Delete an account")
    ResponseEntity<Void> delete(@Positive Long id);
}
