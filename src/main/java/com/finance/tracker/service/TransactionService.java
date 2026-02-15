package com.finance.tracker.service;

import com.finance.tracker.dto.TransactionDto;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {

    TransactionDto getById(Long id);

    List<TransactionDto> getByDateRange(final LocalDate startDate, final LocalDate endDate);
}
