package com.finance.tracker.service.impl;

import com.finance.tracker.domain.AsyncTransactionTask;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.exception.LoggingException;
import com.finance.tracker.service.TransactionService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AsyncTransactionExecutorService {

    private final AsyncTaskStorage asyncTaskStorage;
    private final TransactionService transactionService;
    private final TransactionTemplate transactionTemplate;
    private final long initialDelayMillis;
    private final long perItemDelayMillis;

    public AsyncTransactionExecutorService(
            AsyncTaskStorage asyncTaskStorage,
            TransactionService transactionService,
            PlatformTransactionManager transactionManager,
            @Value("${lab6.async.initial-delay-ms:1000}") long initialDelayMillis,
            @Value("${lab6.async.per-item-delay-ms:700}") long perItemDelayMillis) {
        this.asyncTaskStorage = asyncTaskStorage;
        this.transactionService = transactionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.initialDelayMillis = initialDelayMillis;
        this.perItemDelayMillis = perItemDelayMillis;
    }

    @Async
    public CompletableFuture<Void> executeTransactionsCreation(
            String taskId,
            List<TransactionRequest> requests,
            boolean transactional) {
        AsyncTransactionTask task = asyncTaskStorage.getTask(taskId);
        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }

        task.markInProgress();
        
        try {
            Thread.sleep(10000);
            pause(initialDelayMillis);

            if (transactional) {
                List<Long> createdTransactionIds = executeTransactionalImport(task, requests);
                task.addCreatedTransactionIds(createdTransactionIds);
            } else {
                executeNonTransactionalImport(task, requests);
            }

            task.markCompleted("Created " + task.toResponse().getCreatedTransactionIds().size() + " transactions");
        } catch (Exception exception) {
            if (transactional) {
                task.clearCreatedTransactionIds();
            }
            task.markFailed(resolveMessage(exception));
        }

        return CompletableFuture.completedFuture(null);
    }

    private List<Long> executeTransactionalImport(AsyncTransactionTask task, List<TransactionRequest> requests) {
        List<Long> createdTransactionIds = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> {
            for (TransactionRequest request : requests) {
                TransactionResponse response = transactionService.create(request);
                createdTransactionIds.add(response.getId());
                task.incrementProcessedItems();
                pause(perItemDelayMillis);
            }
        });
        return createdTransactionIds;
    }

    private void executeNonTransactionalImport(AsyncTransactionTask task, List<TransactionRequest> requests) {
        for (TransactionRequest request : requests) {
            TransactionResponse response = transactionService.create(request);
            task.incrementProcessedItems();
            task.addCreatedTransactionId(response.getId());
            pause(perItemDelayMillis);
        }
    }

    private void pause(long delayMillis) {
        if (delayMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LoggingException("Async transaction import interrupted");
        }
    }

    private String resolveMessage(Exception exception) {
        String message = exception.getMessage();
        return (message == null || message.isBlank()) ? exception.getClass().getSimpleName() : message;
    }
}
