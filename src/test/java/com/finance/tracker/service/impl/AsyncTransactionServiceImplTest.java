package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finance.tracker.domain.AsyncTaskStatus;
import com.finance.tracker.domain.AsyncTransactionTask;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.AsyncTaskStatusResponse;
import com.finance.tracker.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncTransactionServiceImplTest {

    @Mock
    private AsyncTaskStorage asyncTaskStorage;

    @Mock
    private AsyncTransactionExecutorService asyncTransactionExecutor;

    private AsyncTransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AsyncTransactionServiceImpl(asyncTaskStorage, asyncTransactionExecutor);
    }

    @Test
    void createTransactionsAsyncShouldSavePendingTaskAndStartExecution() {
        List<TransactionRequest> requests = List.of(request("Coffee"), request("Taxi"));

        String taskId = service.createTransactionsAsync(requests, true);

        ArgumentCaptor<AsyncTransactionTask> taskCaptor = ArgumentCaptor.forClass(AsyncTransactionTask.class);
        verify(asyncTaskStorage).saveTask(taskCaptor.capture());
        AsyncTransactionTask savedTask = taskCaptor.getValue();

        assertEquals(taskId, savedTask.getTaskId());
        assertEquals(AsyncTaskStatus.PENDING, savedTask.getStatus());
        assertEquals(2, savedTask.getTotalItems());
        assertTrue(savedTask.isTransactional());
        assertNotNull(savedTask.getCreatedAt());
        verify(asyncTransactionExecutor).executeTransactionsCreation(taskId, requests, true);
    }

    @Test
    void createTransactionsAsyncShouldRejectNullRequests() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createTransactionsAsync(null, false));

        assertTrue(exception.getMessage().contains("at least one item"));
        verifyNoInteractions(asyncTaskStorage, asyncTransactionExecutor);
    }

    @Test
    void createTransactionsAsyncShouldRejectEmptyRequests() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createTransactionsAsync(List.of(), false));

        assertTrue(exception.getMessage().contains("at least one item"));
        verifyNoInteractions(asyncTaskStorage, asyncTransactionExecutor);
    }

    @Test
    void getTransactionTaskStatusShouldReturnTaskSnapshotFromStorage() {
        AsyncTransactionTask task = new AsyncTransactionTask(
                "task-1",
                false,
                1,
                LocalDateTime.of(2026, 4, 6, 12, 0));
        task.markInProgress();
        task.incrementProcessedItems();
        when(asyncTaskStorage.getTask("task-1")).thenReturn(task);

        AsyncTaskStatusResponse response = service.getTransactionTaskStatus("task-1");

        assertEquals("task-1", response.getTaskId());
        assertEquals(AsyncTaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(1, response.getProcessedItems());
        assertEquals(100, response.getProgressPercent());
    }

    @Test
    void getTransactionTaskStatusShouldReturnNullForMissingTask() {
        when(asyncTaskStorage.getTask("missing")).thenReturn(null);

        AsyncTaskStatusResponse response = service.getTransactionTaskStatus("missing");

        assertNull(response);
    }

    @Test
    void getAllAsyncTasksShouldReturnSnapshotsSortedByCreationTimeDescending() {
        AsyncTransactionTask olderTask = new AsyncTransactionTask(
                "task-1",
                true,
                1,
                LocalDateTime.of(2026, 4, 6, 12, 0));
        AsyncTransactionTask newerTask = new AsyncTransactionTask(
                "task-2",
                false,
                1,
                LocalDateTime.of(2026, 4, 6, 12, 1));
        Map<String, AsyncTransactionTask> tasks = Map.of(
                "task-1", olderTask,
                "task-2", newerTask);
        when(asyncTaskStorage.getAllTasks()).thenReturn(tasks);

        Map<String, AsyncTaskStatusResponse> result = service.getAllAsyncTasks();

        assertEquals(List.of("task-2", "task-1"), List.copyOf(result.keySet()));
        assertSame(AsyncTaskStatus.PENDING, result.get("task-2").getStatus());
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
}
