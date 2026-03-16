package com.finance.tracker.cache;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionSearchIndexInvalidator {

    private final TransactionSearchIndex transactionSearchIndex;

    public void invalidateAfterCommitOrNow() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            int removedEntries = transactionSearchIndex.invalidateAll();
            log.info("Transaction search cache INVALIDATED immediately [entriesRemoved={}]", removedEntries);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                int removedEntries = transactionSearchIndex.invalidateAll();
                log.info("Transaction search cache INVALIDATED after commit [entriesRemoved={}]", removedEntries);
            }
        });
    }
}
