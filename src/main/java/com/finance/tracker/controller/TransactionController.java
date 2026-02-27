package com.finance.tracker.controller;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
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
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService service;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable final Long id) {
        return ResponseEntity.ok(service.getTransactionById(id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getByDateRange(
            @RequestParam(required = false) final LocalDate startDate,
            @RequestParam(required = false) final LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") final boolean withEntityGraph) {
        if (startDate == null && endDate == null) {
            return ResponseEntity.ok(service.getAllTransactions(withEntityGraph));
        }
        return ResponseEntity.ok(service.getTransactionsByDateRange(startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = service.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable Long id,
            @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.updateTransaction(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        service.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
