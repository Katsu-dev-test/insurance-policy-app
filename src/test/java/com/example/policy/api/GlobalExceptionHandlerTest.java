package com.example.policy.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.policy.domain.PolicyNotFoundException;
import com.example.policy.domain.PolicyStatus;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 契約未存在は404と汎用メッセージを返す() {
        ProblemDetail problem = handler.handleNotFound(new PolicyNotFoundException("P9999999"));

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Policy not found", problem.getDetail());
    }

    @Test
    void 契約未存在の応答に契約番号を含めない() {
        ProblemDetail problem = handler.handleNotFound(new PolicyNotFoundException("P9999999"));

        assertFalse(problem.getDetail().contains("P9999999"));
    }

    @Test
    void 入力不正は400と汎用メッセージを返す() {
        ProblemDetail problem = handler.handleInvalidInput(
                new ConstraintViolationException("getPolicy.policyNumber: must match pattern", Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Invalid request", problem.getDetail());
    }

    @Test
    void 入力不正の応答に例外の内部メッセージを含めない() {
        ProblemDetail problem = handler.handleInvalidInput(
                new ConstraintViolationException("getPolicy.policyNumber: must match pattern", Set.of()));

        assertFalse(problem.getDetail().contains("pattern"));
    }

    @Test
    void 不正なステータス値は400と汎用メッセージを返す() {
        ProblemDetail problem = handler.handleInvalidStatus(new InvalidStatusException("UNKNOWN"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Invalid request", problem.getDetail());
    }

    @Test
    void 不正なステータスの応答に指定した値を含めない() {
        ProblemDetail problem = handler.handleInvalidStatus(new InvalidStatusException("UNKNOWN"));

        assertFalse(problem.getDetail().contains("UNKNOWN"));
    }
}
