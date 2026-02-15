package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Transaction;
import com.finance.tracker.dto.TransactionDto;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.service.TransactionService;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;

    @Override
    public TransactionDto getById(final Long id) {
        return TransactionMapper.toDto(repository.findById(id).get());
    }

    @Override
    public List<TransactionDto> getByDateRange(final LocalDate startDate, final LocalDate endDate) {
        List<Transaction> allTransactions = repository.findAll();

        if (startDate == null && endDate == null) {
            return allTransactions.stream().map(TransactionMapper::toDto).toList();
        }

        return allTransactions.stream()
                .filter(tx -> {
                    var date = tx.getDate();
                    return date.isAfter(startDate) && date.isBefore(endDate);
                }).map(TransactionMapper::toDto)
                .toList();
    }
}
