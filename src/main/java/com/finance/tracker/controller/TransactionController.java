package com.finance.tracker.controller;

import com.finance.tracker.controller.api.TransactionControllerApi;
import com.finance.tracker.dto.request.TransactionSearchRequest;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionSearchPageResponse;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.dto.response.TransactionSearchResult;
import com.finance.tracker.service.TransactionService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController implements TransactionControllerApi {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<TransactionResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping
    @Override
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
    @Override
    public ResponseEntity<TransactionSearchPageResponse> search(
            @ModelAttribute TransactionSearchRequest request) {
        TransactionSearchResult result = transactionService.search(request.toCriteria(), request.toPageable());
        return ResponseEntity.ok()
                .header("X-Transaction-Search-Source", result.getSource().name())
                .body(TransactionSearchPageResponse.from(result.getPage()));
    }

    @PostMapping
    @Override
    public ResponseEntity<TransactionResponse> create(@RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @Override
    public ResponseEntity<List<TransactionResponse>> createBulk(
            @RequestBody List<TransactionRequest> requests,
            @RequestParam(defaultValue = "true") boolean transactional) {
        List<TransactionResponse> response = transactional
                ? transactionService.createBulkTx(requests)
                : transactionService.createBulkNoTx(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<TransactionResponse> update(
            @PathVariable("id") Long id,
            @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @PatchMapping("/{id}")
    @Override
    public ResponseEntity<TransactionResponse> patch(
            @PathVariable("id") Long id,
            @RequestBody TransactionUpdateRequest request) {
        return ResponseEntity.ok(transactionService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
