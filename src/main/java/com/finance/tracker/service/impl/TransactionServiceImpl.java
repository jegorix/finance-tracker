package com.finance.tracker.service.impl;

import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.service.TransactionService;
import java.util.List;
import org.springframework.stereotype.Service;


@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;

    public TransactionServiceImpl(TransactionRepository repository) {
        this.repository = repository;
    }


    @Override
    public TransactionResponse getById(Long id) {
        final Transaction transaction = repository.findById(id).orElseThrow();
        return TransactionMapper.toResponse(transaction);
    }


    @Override
    public List<TransactionResponse> getByCategory(String category) {
        return repository.findByCategoryIgnoreCase(category)
            .stream()
            .map(TransactionMapper::toResponse)
            .toList();
    }

}
