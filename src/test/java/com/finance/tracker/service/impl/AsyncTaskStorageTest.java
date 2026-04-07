package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.finance.tracker.domain.AsyncTransactionTask;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncTaskStorageTest {

    private AsyncTaskStorage storage;

    @BeforeEach
    void setUp() {
        storage = new AsyncTaskStorage();
    }

    @Test
    void saveTaskShouldStoreTaskById() {
        AsyncTransactionTask task = new AsyncTransactionTask(
                "task-1",
                true,
                2,
                LocalDateTime.of(2026, 4, 6, 12, 0));

        storage.saveTask(task);

        assertSame(task, storage.getTask("task-1"));
    }

    @Test
    void getAllTasksShouldReturnAllSavedTasks() {
        AsyncTransactionTask firstTask = new AsyncTransactionTask(
                "task-1",
                true,
                2,
                LocalDateTime.of(2026, 4, 6, 12, 0));
        AsyncTransactionTask secondTask = new AsyncTransactionTask(
                "task-2",
                false,
                3,
                LocalDateTime.of(2026, 4, 6, 12, 1));
        storage.saveTask(firstTask);
        storage.saveTask(secondTask);

        var tasks = storage.getAllTasks();

        assertEquals(2, tasks.size());
        assertSame(firstTask, tasks.get("task-1"));
        assertSame(secondTask, tasks.get("task-2"));
    }
}
