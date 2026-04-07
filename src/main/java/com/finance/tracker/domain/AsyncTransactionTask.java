package com.finance.tracker.domain;

import com.finance.tracker.dto.response.AsyncTaskStatusResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class AsyncTransactionTask {

    @Getter(AccessLevel.NONE)
    private final List<Long> createdTransactionIds = Collections.synchronizedList(new ArrayList<>());
    @Getter(AccessLevel.NONE)
    private final AtomicInteger processedItems = new AtomicInteger();

    private final String taskId;
    private final boolean transactional;
    private final int totalItems;
    private final LocalDateTime createdAt;

    private volatile AsyncTaskStatus status;
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime completedAt;
    private volatile String summary;
    private volatile String errorMessage;

    public AsyncTransactionTask(String taskId, boolean transactional, int totalItems, LocalDateTime createdAt) {
        this.taskId = taskId;
        this.transactional = transactional;
        this.totalItems = totalItems;
        this.createdAt = createdAt;
        this.status = AsyncTaskStatus.PENDING;
    }

    public void markInProgress() {
        status = AsyncTaskStatus.IN_PROGRESS;
        startedAt = LocalDateTime.now();
        completedAt = null;
        errorMessage = null;
        summary = null;
    }

    public void incrementProcessedItems() {
        processedItems.incrementAndGet();
    }

    public void addCreatedTransactionId(Long transactionId) {
        if (transactionId == null) {
            return;
        }
        synchronized (createdTransactionIds) {
            createdTransactionIds.add(transactionId);
        }
    }

    public void addCreatedTransactionIds(List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return;
        }
        synchronized (createdTransactionIds) {
            createdTransactionIds.addAll(transactionIds.stream()
                    .filter(Objects::nonNull)
                    .toList());
        }
    }

    public void clearCreatedTransactionIds() {
        synchronized (createdTransactionIds) {
            createdTransactionIds.clear();
        }
    }

    public int getProcessedItems() {
        return processedItems.get();
    }

    public int getProgressPercent() {
        if (totalItems == 0) {
            return 100;
        }
        return Math.min(100, processedItems.get() * 100 / totalItems);
    }

    public AsyncTaskStatusResponse toResponse() {
        return new AsyncTaskStatusResponse(
                taskId,
                status,
                transactional,
                totalItems,
                getProcessedItems(),
                getProgressPercent(),
                getCreatedTransactionIdsSnapshot(),
                summary,
                errorMessage,
                createdAt,
                startedAt,
                completedAt);
    }

    public void markCompleted(String taskSummary) {
        summary = taskSummary;
        errorMessage = null;
        status = AsyncTaskStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    public void markFailed(String taskErrorMessage) {
        summary = null;
        errorMessage = taskErrorMessage;
        status = AsyncTaskStatus.FAILED;
        completedAt = LocalDateTime.now();
    }

    private List<Long> getCreatedTransactionIdsSnapshot() {
        synchronized (createdTransactionIds) {
            return List.copyOf(createdTransactionIds);
        }
    }
}
