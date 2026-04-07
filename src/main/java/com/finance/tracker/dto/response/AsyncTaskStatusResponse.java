package com.finance.tracker.dto.response;

import com.finance.tracker.domain.AsyncTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Snapshot of the asynchronous bulk transaction import status.")
public class AsyncTaskStatusResponse {

    private String taskId;
    private AsyncTaskStatus status;
    private boolean transactional;
    private int totalItems;
    private int processedItems;
    private int progressPercent;
    private List<Long> createdTransactionIds;
    private String summary;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
