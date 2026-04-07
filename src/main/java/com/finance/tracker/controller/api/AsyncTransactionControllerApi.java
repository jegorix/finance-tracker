package com.finance.tracker.controller.api;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.AsyncTaskStatusResponse;
import com.finance.tracker.dto.response.AsyncTaskSubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;

@Tag(name = "Async Transactions", description = "Laboratory 6 asynchronous bulk transaction demo")
public interface AsyncTransactionControllerApi {

    @Operation(summary = "Start asynchronous bulk transaction creation")
    ResponseEntity<AsyncTaskSubmissionResponse> createTransactionsAsync(
            @Valid List<@Valid TransactionRequest> requests,
            @Parameter(description = "Run the whole bulk import in one transaction") boolean transactional);

    @Operation(summary = "Get status of asynchronous bulk transaction creation")
    ResponseEntity<AsyncTaskStatusResponse> getTaskStatus(String taskId);

    @Operation(summary = "Get snapshots of all asynchronous bulk transaction tasks")
    ResponseEntity<Map<String, AsyncTaskStatusResponse>> getAllAsyncTasks();
}
