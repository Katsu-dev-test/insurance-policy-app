package com.example.policy.api;

import com.example.policy.domain.PolicyNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * クライアントには汎用メッセージのみ返し、詳細はサーバーログにのみ出力する。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PolicyNotFoundException.class)
    public ProblemDetail handleNotFound(PolicyNotFoundException e) {
        log.info("Policy not found");
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Policy not found");
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ProblemDetail handleInvalidInput(Exception e) {
        log.info("Invalid policy number format");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ProblemDetail handleInvalidStatus(InvalidStatusException e) {
        log.info("Invalid status value");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
    }
}
