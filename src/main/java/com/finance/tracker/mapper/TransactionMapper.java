package com.finance.tracker.mapper;

import com.finance.tracker.domain.Transaction;
import com.finance.tracker.dto.TransactionDto;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public static TransactionDto toDto(final Transaction entity) {
        if (entity == null) {
            return null;
        }
        return new TransactionDto(entity.getId(), entity.getAmount(), entity.getDate(), entity.getDescription());
    }

    public static Transaction toDomain(final TransactionDto dto) {
        if (dto == null) {
            return null;
        }
        return new Transaction(dto.getId(), dto.getAmount(), dto.getDate(), dto.getDescription());
    }
}
