package com.finance.tracker.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Detailed validation error for a single request field or parameter")
public class ApiErrorField {

    @Schema(description = "Field or parameter name", example = "email")
    private final String field;

    @Schema(description = "Validation or conversion error message", example = "must be a well-formed email address")
    private final String message;

    @Schema(description = "Rejected value converted to string", example = "not-an-email")
    private final String rejectedValue;
}
