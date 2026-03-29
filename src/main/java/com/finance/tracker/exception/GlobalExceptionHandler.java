package com.finance.tracker.exception;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
    private static final String REQUEST_BODY_INVALID_CODE = "REQUEST_BODY_INVALID";
    private static final String TYPE_MISMATCH_CODE = "TYPE_MISMATCH";
    private static final String DATABASE_CONFLICT_CODE = "DATABASE_CONFLICT";
    private static final String METHOD_NOT_ALLOWED_CODE = "METHOD_NOT_ALLOWED";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final String REQUEST_VALIDATION_FAILED_MESSAGE = "Request validation failed";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException exception,
            HttpServletRequest request) {
        HttpStatus status = exception.getStatus();
        ApiErrorResponse response = buildError(
                status,
                codeForStatus(status),
                exception.getMessage(),
                request.getRequestURI(),
                List.of());
        logByStatus(status, "Handled api exception at {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        return buildValidationError(
                HttpStatus.BAD_REQUEST,
                REQUEST_VALIDATION_FAILED_MESSAGE,
                request,
                extractBindingErrors(exception.getBindingResult()),
                exception);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
            BindException exception,
            HttpServletRequest request) {
        return buildValidationError(
                HttpStatus.BAD_REQUEST,
                REQUEST_VALIDATION_FAILED_MESSAGE,
                request,
                extractBindingErrors(exception.getBindingResult()),
                exception);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {
        return buildValidationError(
                HttpStatus.BAD_REQUEST,
                REQUEST_VALIDATION_FAILED_MESSAGE,
                request,
                extractParameterErrors(exception),
                exception);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ApiErrorField> fieldErrors = exception.getConstraintViolations().stream()
                .map(this::toApiErrorField)
                .sorted(Comparator.comparing(ApiErrorField::getField))
                .toList();
        return buildValidationError(
                HttpStatus.BAD_REQUEST,
                REQUEST_VALIDATION_FAILED_MESSAGE,
                request,
                fieldErrors,
                exception);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        ApiErrorField fieldError = new ApiErrorField(
                exception.getParameterName(),
                exception.getMessage(),
                null);
        return buildValidationError(
                HttpStatus.BAD_REQUEST,
                "Required request parameter is missing",
                request,
                List.of(fieldError),
                exception);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        String fieldName = exception.getName();
        String expectedType = exception.getRequiredType() == null
                ? "the required type"
                : exception.getRequiredType().getSimpleName();
        ApiErrorField fieldError = new ApiErrorField(
                fieldName,
                "Value must match type " + expectedType,
                stringifyValue(exception.getValue()));
        ApiErrorResponse response = buildError(
                HttpStatus.BAD_REQUEST,
                TYPE_MISMATCH_CODE,
                "Request parameter has invalid type",
                request.getRequestURI(),
                List.of(fieldError));
        log.warn("Type mismatch at {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        List<ApiErrorField> fieldErrors = extractJsonErrors(exception);
        ApiErrorResponse response = buildError(
                HttpStatus.BAD_REQUEST,
                REQUEST_BODY_INVALID_CODE,
                "Request body is malformed or contains unsupported values",
                request.getRequestURI(),
                fieldErrors);
        log.warn("Unreadable request body at {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        ApiErrorResponse response = buildError(
                HttpStatus.CONFLICT,
                DATABASE_CONFLICT_CODE,
                "Operation violates database constraints",
                request.getRequestURI(),
                List.of());
        log.warn("Database constraint violation at {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        ApiErrorResponse response = buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                METHOD_NOT_ALLOWED_CODE,
                exception.getMessage(),
                request.getRequestURI(),
                List.of());
        log.warn("Method not allowed at {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        ApiErrorResponse response = buildError(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Endpoint not found",
                request.getRequestURI(),
                List.of());
        log.warn("Endpoint not found at {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        HttpStatusCode statusCode = exception.getStatusCode();
        ApiErrorResponse response = buildError(
                statusCode,
                codeForStatus(statusCode),
                Optional.ofNullable(exception.getReason()).orElse("Request processing failed"),
                request.getRequestURI(),
                List.of());
        logByStatus(
                statusCode,
                "Handled business exception at {}: {}",
                request.getRequestURI(),
                exception.getMessage());
        return ResponseEntity.status(statusCode).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request) {
        ApiErrorResponse response = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_CODE,
                "Unexpected server error",
                request.getRequestURI(),
                List.of());
        log.error("Unexpected error at {}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ApiErrorResponse> buildValidationError(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<ApiErrorField> fieldErrors,
            Exception exception) {
        ApiErrorResponse response = buildError(
                status,
                VALIDATION_ERROR_CODE,
                message,
                request.getRequestURI(),
                fieldErrors);
        log.warn("Validation failed at {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    private ApiErrorResponse buildError(
            HttpStatusCode statusCode,
            String code,
            String message,
            String path,
            List<ApiErrorField> fieldErrors) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                statusCode.value(),
                resolveReasonPhrase(statusCode),
                code,
                message,
                path,
                fieldErrors);
    }

    private List<ApiErrorField> extractBindingErrors(BindingResult bindingResult) {
        Stream<ApiErrorField> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(this::toApiErrorField);
        Stream<ApiErrorField> globalErrors = bindingResult.getGlobalErrors().stream()
                .map(error -> new ApiErrorField(error.getObjectName(), defaultMessage(error), null));
        return Stream.concat(fieldErrors, globalErrors)
                .sorted(Comparator.comparing(ApiErrorField::getField))
                .toList();
    }

    private List<ApiErrorField> extractParameterErrors(HandlerMethodValidationException exception) {
        List<ApiErrorField> errors = new ArrayList<>();
        for (ParameterValidationResult validationResult : exception.getParameterValidationResults()) {
            if (validationResult instanceof ParameterErrors parameterErrors) {
                errors.addAll(parameterErrors.getFieldErrors().stream()
                        .map(this::toApiErrorField)
                        .toList());
                errors.addAll(parameterErrors.getGlobalErrors().stream()
                        .map(error -> new ApiErrorField(
                                parameterErrors.getObjectName(),
                                defaultMessage(error),
                                null))
                        .toList());
                continue;
            }

            String parameterName = resolveParameterName(validationResult);
            for (MessageSourceResolvable resolvable : validationResult.getResolvableErrors()) {
                errors.add(new ApiErrorField(
                        parameterName,
                        defaultMessage(resolvable),
                        stringifyValue(validationResult.getArgument())));
            }
        }
        return errors.stream()
                .sorted(Comparator.comparing(ApiErrorField::getField))
                .toList();
    }

    private List<ApiErrorField> extractJsonErrors(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (!(cause instanceof InvalidFormatException invalidFormatException)) {
            return List.of();
        }

        String fieldPath = invalidFormatException.getPath().stream()
                .map(this::toPathSegment)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
        if (fieldPath.isBlank()) {
            fieldPath = "requestBody";
        }

        ApiErrorField fieldError = new ApiErrorField(
                fieldPath,
                "Unsupported value or invalid format",
                stringifyValue(invalidFormatException.getValue()));
        return List.of(fieldError);
    }

    private String toPathSegment(Reference reference) {
        if (reference.getFieldName() != null) {
            return reference.getFieldName();
        }
        return reference.getIndex() >= 0 ? "[" + reference.getIndex() + "]" : null;
    }

    private ApiErrorField toApiErrorField(FieldError fieldError) {
        return new ApiErrorField(
                fieldError.getField(),
                defaultMessage(fieldError),
                stringifyValue(fieldError.getRejectedValue()));
    }

    private ApiErrorField toApiErrorField(ConstraintViolation<?> violation) {
        String field = StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                .map(pathNode -> pathNode.getName())
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse("request");
        return new ApiErrorField(
                field,
                violation.getMessage(),
                stringifyValue(violation.getInvalidValue()));
    }

    private String resolveParameterName(ParameterValidationResult validationResult) {
        String parameterName = validationResult.getMethodParameter().getParameterName();
        if (parameterName != null && !parameterName.isBlank()) {
            return parameterName;
        }
        return "arg" + validationResult.getMethodParameter().getParameterIndex();
    }

    private String defaultMessage(MessageSourceResolvable resolvable) {
        return Optional.ofNullable(resolvable.getDefaultMessage()).orElse("Validation error");
    }

    private String stringifyValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveReasonPhrase(HttpStatusCode statusCode) {
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        return httpStatus == null ? "HTTP " + statusCode.value() : httpStatus.getReasonPhrase();
    }

    private String codeForStatus(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "RESOURCE_NOT_FOUND";
            case 405 -> METHOD_NOT_ALLOWED_CODE;
            case 409 -> "CONFLICT";
            default -> statusCode.is5xxServerError() ? INTERNAL_ERROR_CODE : "REQUEST_ERROR";
        };
    }

    private void logByStatus(HttpStatusCode statusCode, String message, Object... arguments) {
        if (statusCode.is5xxServerError()) {
            log.error(message, arguments);
            return;
        }
        log.warn(message, arguments);
    }
}
