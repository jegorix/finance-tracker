package com.finance.tracker.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.finance.tracker.dto.response.TransactionResponse;

@Component
public class TransactionSearchIndex {

    private final Map<TransactionSearchCacheKey, Page<TransactionResponse>> entries = new HashMap<>();

    public synchronized Optional<Page<TransactionResponse>> find(TransactionSearchCacheKey key) {
        return Optional.ofNullable(entries.get(key))
                .map(this::copyPage);
    }

    public synchronized void put(TransactionSearchCacheKey key, Page<TransactionResponse> page) {
        entries.put(key, copyPage(page));
    }

    public synchronized int invalidateAll() {
        int removedEntries = entries.size();
        entries.clear();
        return removedEntries;
    }

    private Page<TransactionResponse> copyPage(Page<TransactionResponse> page) {
        return new PageImpl<>(
                List.copyOf(page.getContent()),
                page.getPageable(),
                page.getTotalElements());
    }
}
