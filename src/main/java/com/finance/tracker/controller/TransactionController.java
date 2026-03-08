package com.finance.tracker.controller;

import com.finance.tracker.dto.request.TransactionPatchRequest;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable final Long id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startDateTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endDateTime,
            @RequestParam(required = false, defaultValue = "false") final boolean withEntityGraph) {
        if (startDateTime == null && endDateTime == null) {
            return ResponseEntity.ok(transactionService.findAll(withEntityGraph));
        }
        return ResponseEntity.ok(transactionService.findByDateRange(startDateTime, endDateTime));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> patch(
            @PathVariable("id") Long id,
            @Valid @RequestBody TransactionPatchRequest request) {
        return ResponseEntity.ok(transactionService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
