package com.finance.tracker.dto.response;

import com.finance.tracker.domain.AsyncTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned after starting an asynchronous bulk transaction import.")
public class AsyncTaskSubmissionResponse {

    private String taskId;
    private AsyncTaskStatus status;
    private String statusUrl;
}
