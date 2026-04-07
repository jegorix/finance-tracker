package com.finance.tracker.service.impl;

import com.finance.tracker.domain.AsyncTransactionTask;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.AsyncTaskStatusResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.service.AsyncTransactionService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncTransactionServiceImpl implements AsyncTransactionService {

    private static final String BULK_REQUEST_EMPTY_MESSAGE =
            "Bulk transaction request must contain at least one item";

    private final AsyncTaskStorage asyncTaskStorage;
    private final AsyncTransactionExecutorService asyncTransactionExecutor;

    @Override
    public String createTransactionsAsync(List<TransactionRequest> requests, boolean transactional) {
        List<TransactionRequest> bulkRequests = Optional.ofNullable(requests)
                .filter(items -> !items.isEmpty())
                .orElseThrow(() -> new BadRequestException(BULK_REQUEST_EMPTY_MESSAGE));

        String taskId = UUID.randomUUID().toString();
        AsyncTransactionTask task = new AsyncTransactionTask(
                taskId,
                transactional,
                bulkRequests.size(),
                LocalDateTime.now());

        asyncTaskStorage.saveTask(task);
        asyncTransactionExecutor.executeTransactionsCreation(taskId, bulkRequests, transactional);
        return taskId;
    }

    @Override
    public AsyncTaskStatusResponse getTransactionTaskStatus(String taskId) {
        AsyncTransactionTask task = asyncTaskStorage.getTask(taskId);
        return task == null ? null : task.toResponse();
    }

    @Override
    public Map<String, AsyncTaskStatusResponse> getAllAsyncTasks() {
        Map<String, AsyncTaskStatusResponse> responses = new LinkedHashMap<>();
        asyncTaskStorage.getAllTasks().entrySet().stream()
                .sorted((left, right) -> right.getValue().getCreatedAt().compareTo(left.getValue().getCreatedAt()))
                .forEach(entry -> responses.put(entry.getKey(), entry.getValue().toResponse()));
        return responses;
    }
}
