package com.finance.tracker.exception;

import org.springframework.http.HttpStatus;

public class LoggingException extends ApiException {

    public LoggingException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
