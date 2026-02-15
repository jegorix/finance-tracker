package com.finance.tracker.controller;

import com.finance.tracker.domain.Transaction;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.service.TransactionService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/transactions")
public class TransactionController {

    @Autowired
    private final TransactionService service;

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable final Long id) {
        try {
            return ResponseEntity.ok(TransactionMapper.toDomain(service.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getByDateRange(
            @RequestParam(required = false) final LocalDate startDate,
            @RequestParam(required = false) final LocalDate endDate) {
        System.out.println(startDate);
        return ResponseEntity
                .ok(service.getByDateRange(startDate, endDate).stream().map(TransactionMapper::toDomain).toList());
    }
}
