package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.AsyncTaskStatusResponse;
import java.util.List;
import java.util.Map;

public interface AsyncTransactionService {

    String createTransactionsAsync(List<TransactionRequest> requests, boolean transactional);

    AsyncTaskStatusResponse getTransactionTaskStatus(String taskId);

    Map<String, AsyncTaskStatusResponse> getAllAsyncTasks();
}
