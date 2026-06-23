package org.example.tay.taskmanagmentkafkareact.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;


// SHARED LAYER — Global Exception Handler
//
// Centralised error handling for all exceptions.
//
//  REACTIVE VERSION:
//  - Uses ServerWebExchange instead of HttpServletRequest
//    (ServerWebExchange is the WebFlux equivalent of HttpServletRequest)
//  - Returns Mono<ResponseEntity<ErrorResponse>> instead of ResponseEntity
//  - Handles IllegalArgumentException for duplicate Task ID (conflict)

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ────────────────────────────────────────────
    @ExceptionHandler(TaskNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(
            TaskNotFoundException ex, ServerWebExchange exchange) {
        log.error("Task not found: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildError(HttpStatus.NOT_FOUND, "Not Found",
                        ex.getMessage(), exchange.getRequest().getPath().value())));
    }

    // ── 409 Conflict — business rule violation ───────────────────
    @ExceptionHandler(ConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflict(
            ConflictException ex, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError(HttpStatus.CONFLICT, "Conflict",
                        ex.getMessage(), exchange.getRequest().getPath().value())));
    }

    // ── 409 Conflict — status transition violation ───────────────
    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalState(
            IllegalStateException ex, ServerWebExchange exchange) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(HttpStatus.CONFLICT, "Conflict",
                        ex.getMessage(), exchange.getRequest().getPath().value())));
    }

    // ── 403 Forbidden — ownership or role violation ──────────────
    // Thrown by TaskServiceImpl when a USER tries to modify
    // a task they do not own, or when COMPLETED task is modified.
    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleForbidden(
            AccessDeniedException ex, ServerWebExchange exchange) {
        log.warn("Access denied for path {}: {}", exchange.getRequest().getPath().value(), ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(HttpStatus.FORBIDDEN, "Forbidden",
                        ex.getMessage(), exchange.getRequest().getPath().value())));
    }

    // ── 401 Unauthorized — missing or expired JWT ────────────────
    // Spring Security intercepts most 401s before reaching here,
    // but service-layer auth failures bubble up through this handler.
    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnauthorized(
            AuthenticationException ex, ServerWebExchange exchange) {
        log.warn("Authentication failure: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildError(HttpStatus.UNAUTHORIZED, "Unauthorized",
                        "Authentication required", exchange.getRequest().getPath().value())));
    }

    // ── 400 Bad Request — @Valid constraint failures ─────────────
    // WebFlux throws WebExchangeBindException (not MethodArgumentNotValidException)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("Validation failed: {}", message);
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(HttpStatus.BAD_REQUEST, "Bad Request",
                        message, exchange.getRequest().getPath().value())));
    }

    // ── 500 Internal Server Error — catch-all ────────────────────
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error at {}: {}", exchange.getRequest().getPath().value(), ex.getMessage(), ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                        "An unexpected error occurred", exchange.getRequest().getPath().value())));
    }

    @ExceptionHandler(KafkaPublishException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleKafkaError(
            KafkaPublishException ex, ServerWebExchange exchange) {
        log.error("Kafka publish error: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE) // 503 服务不可用
                .body(buildError(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                        "The message queue is down. Please try again later.",
                        exchange.getRequest().getPath().value())));
    }

    // ── Builder helper ───────────────────────────────────────────
    private ErrorResponse buildError(HttpStatus status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}

