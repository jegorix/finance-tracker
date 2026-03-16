package com.finance.tracker.dto.response;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionSearchResult {

    private final Page<TransactionResponse> page;
    private final TransactionSearchSource source;
}
