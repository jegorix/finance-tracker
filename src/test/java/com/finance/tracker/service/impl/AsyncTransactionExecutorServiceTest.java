package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finance.tracker.domain.AsyncTaskStatus;
import com.finance.tracker.domain.AsyncTransactionTask;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AsyncTransactionExecutorServiceTest {

    @Mock
    private AsyncTaskStorage asyncTaskStorage;

    @Mock
    private TransactionService transactionService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AsyncTransactionExecutorService service;

    @BeforeEach
    void setUp() {
        service = new AsyncTransactionExecutorService(
                asyncTaskStorage,
                transactionService,
                transactionManager,
                0L,
                0L);
    }

    @Test
    void executeTransactionsCreationShouldReturnCompletedFutureWhenTaskIsMissing() {
        CompletableFuture<Void> result =
                service.executeTransactionsCreation("missing", List.of(request("Coffee")), false);

        assertTrue(result.isDone());
        verify(asyncTaskStorage).getTask("missing");
        verifyNoInteractions(transactionService, transactionManager);
    }

    @Test
    void executeTransactionsCreationShouldCompleteNonTransactionalImport() {
        AsyncTransactionTask task = task("task-1", false, 1);
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);
        when(transactionService.create(any(TransactionRequest.class))).thenReturn(response(10L));

        CompletableFuture<Void> result =
                service.executeTransactionsCreation("task-1", List.of(request("Coffee")), false);

        assertTrue(result.isDone());
        assertEquals(AsyncTaskStatus.COMPLETED, task.getStatus());
        assertEquals(1, task.getProcessedItems());
        assertEquals(100, task.getProgressPercent());
        assertEquals(List.of(10L), task.toResponse().getCreatedTransactionIds());
        assertEquals("Created 1 transactions", task.toResponse().getSummary());
        assertNotNull(task.getStartedAt());
        assertNotNull(task.getCompletedAt());
        verify(transactionService).create(any(TransactionRequest.class));
        verifyNoInteractions(transactionManager);
    }

    @Test
    void executeTransactionsCreationShouldCommitTransactionalImport() {
        AsyncTransactionTask task = task("task-1", true, 1);
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(transactionService.create(any(TransactionRequest.class))).thenReturn(response(15L));

        CompletableFuture<Void> result =
                service.executeTransactionsCreation("task-1", List.of(request("Coffee")), true);

        assertTrue(result.isDone());
        assertEquals(AsyncTaskStatus.COMPLETED, task.getStatus());
        assertEquals(1, task.getProcessedItems());
        assertEquals(List.of(15L), task.toResponse().getCreatedTransactionIds());
        assertEquals("Created 1 transactions", task.toResponse().getSummary());
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void executeTransactionsCreationShouldRollbackTransactionalImportOnFailure() {
        AsyncTransactionTask task = task("task-1", true, 1);
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(transactionService.create(any(TransactionRequest.class)))
                .thenThrow(new IllegalStateException("boom"));

        CompletableFuture<Void> result =
                service.executeTransactionsCreation("task-1", List.of(request("Coffee")), true);

        assertTrue(result.isDone());
        assertEquals(AsyncTaskStatus.FAILED, task.getStatus());
        assertEquals("boom", task.getErrorMessage());
        assertEquals(List.of(), task.toResponse().getCreatedTransactionIds());
        assertNotNull(task.getCompletedAt());
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void executeTransactionsCreationShouldMarkTaskFailedWhenInterruptedDuringDelay() {
        AsyncTransactionExecutorService delayedService = new AsyncTransactionExecutorService(
                asyncTaskStorage,
                transactionService,
                transactionManager,
                1L,
                0L);
        AsyncTransactionTask task = task("task-1", false, 1);
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);

        try {
            Thread.currentThread().interrupt();

            CompletableFuture<Void> result =
                    delayedService.executeTransactionsCreation("task-1", List.of(request("Coffee")), false);

            assertTrue(result.isDone());
            assertEquals(AsyncTaskStatus.FAILED, task.getStatus());
            assertEquals("Async transaction import interrupted", task.getErrorMessage());
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(transactionService);
        } finally {
            Thread.interrupted();
        }
    }

    private AsyncTransactionTask task(String taskId, boolean transactional, int totalItems) {
        return new AsyncTransactionTask(
                taskId,
                transactional,
                totalItems,
                LocalDateTime.of(2026, 4, 6, 12, 0));
    }

    private TransactionRequest request(String description) {
        return new TransactionRequest(
                LocalDateTime.of(2026, 4, 5, 12, 0),
                new BigDecimal("10.00"),
                description,
                TransactionType.EXPENSE,
                1L,
                1L);
    }

    private TransactionResponse response(Long id) {
        return new TransactionResponse(
                id,
                LocalDateTime.of(2026, 4, 5, 12, 0),
                new BigDecimal("10.00"),
                "Created",
                TransactionType.EXPENSE,
                1L,
                "Food",
                1L,
                "Main account");
    }
}
