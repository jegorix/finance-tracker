package com.finance.tracker.controller;

import java.math.BigDecimal;
import com.finance.tracker.dto.request.TransactionPatchRequest;
import com.finance.tracker.dto.request.TransactionSearchQueryMode;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionSearchPageResponse;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.dto.response.TransactionSearchResult;
import com.finance.tracker.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/search")
    public ResponseEntity<TransactionSearchPageResponse> search(
            @RequestParam(required = false) final String budgetName,
            @RequestParam(required = false) final String accountName,
            @RequestParam(required = false) final BigDecimal minAmount,
            @RequestParam(required = false) final BigDecimal maxAmount,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startDateTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endDateTime,
            @RequestParam(defaultValue = "JPQL") final TransactionSearchQueryMode queryMode,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "5") final int size,
            @RequestParam(defaultValue = "occurredAt") final String sortBy,
            @RequestParam(defaultValue = "false") final boolean ascending) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be greater than 0");
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        TransactionSearchResult result = transactionService.search(
                queryMode,
                budgetName,
                accountName,
                minAmount,
                maxAmount,
                startDateTime,
                endDateTime,
                pageable);
        return ResponseEntity.ok()
                .header("X-Transaction-Search-Source", result.getSource().name())
                .body(TransactionSearchPageResponse.from(result.getPage()));
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
