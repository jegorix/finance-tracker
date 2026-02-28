package com.finance.tracker.controller;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<TransactionResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/category/{categoryName}")
    public List<TransactionResponse> getByCategory(
        @PathVariable String categoryName,
        @RequestParam(defaultValue = "true") boolean useEntityGraph
    ) {
        return service.getByCategory(categoryName, useEntityGraph);
    }

    @PostMapping
    public TransactionResponse create(@RequestBody @Valid TransactionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @RequestBody @Valid TransactionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/batch/no-tx")
    public void partialSave(@RequestBody @Valid TransactionRequest request) {
        service.demonstratePartialSave(request);
    }

    @PostMapping("/batch/with-tx")
    public void transactionalSave(@RequestBody @Valid TransactionRequest request) {
        service.demonstrateRolledBackSave(request);
    }
}
