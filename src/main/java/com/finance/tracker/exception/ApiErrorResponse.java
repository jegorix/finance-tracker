package com.finance.tracker.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Unified error response returned by the API")
public class ApiErrorResponse {

    @Schema(description = "Timestamp of error creation", example = "2026-03-19T14:25:31.421+03:00")
    private final OffsetDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private final int status;

    @Schema(description = "HTTP status reason", example = "Bad Request")
    private final String error;

    @Schema(description = "Application-specific error code", example = "VALIDATION_ERROR")
    private final String code;

    @Schema(description = "Human-readable error message", example = "Request validation failed")
    private final String message;

    @Schema(description = "Request path", example = "/api/v1/users")
    private final String path;

    @Schema(description = "Field-level validation details")
    private final List<ApiErrorField> fieldErrors;
}
