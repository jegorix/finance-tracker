package com.finance.tracker.service.impl;

import com.finance.tracker.domain.AsyncTransactionTask;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AsyncTaskStorage {

    private final Map<String, AsyncTransactionTask> taskStatuses = new ConcurrentHashMap<>();

    public void saveTask(AsyncTransactionTask task) {
        taskStatuses.put(task.getTaskId(), task);
    }

    public AsyncTransactionTask getTask(String taskId) {
        return taskStatuses.get(taskId);
    }

    public Map<String, AsyncTransactionTask> getAllTasks() {
        return Map.copyOf(taskStatuses);
    }
}
