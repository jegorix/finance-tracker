package com.finance.tracker.controller;

import com.finance.tracker.controller.api.AsyncTransactionControllerApi;
import com.finance.tracker.domain.AsyncTaskStatus;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.AsyncTaskStatusResponse;
import com.finance.tracker.dto.response.AsyncTaskSubmissionResponse;
import com.finance.tracker.service.AsyncTransactionService;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions/bulk/async")
public class AsyncTransactionController implements AsyncTransactionControllerApi {

    private final AsyncTransactionService asyncTransactionService;

    @PostMapping
    @Override
    public ResponseEntity<AsyncTaskSubmissionResponse> createTransactionsAsync(
            @Valid @RequestBody List<@Valid TransactionRequest> requests,
            @RequestParam(defaultValue = "true") boolean transactional) {
        String taskId = asyncTransactionService.createTransactionsAsync(requests, transactional);
        String statusUrl = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{taskId}")
                .buildAndExpand(taskId)
                .toUriString();
        AsyncTaskSubmissionResponse response = new AsyncTaskSubmissionResponse(
                taskId,
                AsyncTaskStatus.PENDING,
                statusUrl);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{taskId}")
    @Override
    public ResponseEntity<AsyncTaskStatusResponse> getTaskStatus(@PathVariable("taskId") String taskId) {
        AsyncTaskStatusResponse task = asyncTransactionService.getTransactionTaskStatus(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @GetMapping
    @Override
    public ResponseEntity<Map<String, AsyncTaskStatusResponse>> getAllAsyncTasks() {
        return ResponseEntity.ok(asyncTransactionService.getAllAsyncTasks());
    }
}
