package org.example.tay.springbkafkamongodbdemo.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(TaskNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTaskNotFoundException(
            TaskNotFoundException ex, ServerWebExchange exchange) {
        log.error("Task not found: {}", ex.getMessage());
        ErrorResponse error = buildError(
                HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflict(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        log.warn("Conflict: {}", ex.getMessage());
        ErrorResponse error = buildError(
                HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("Validation failed: {}", message);
        ErrorResponse error = buildError(
                HttpStatus.BAD_REQUEST, "Bad Request",
                message, exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidEnumException(
            HttpMessageNotReadableException ex, ServerWebExchange exchange) {
        log.error("Invalid request body: {}", ex.getMessage());
        ErrorResponse error = buildError(
                HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid value for status or priority. Allowed: PENDING/IN_PROGRESS/COMPLETED, LOW/MEDIUM/HIGH",
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse error = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred",
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalState(
            IllegalStateException ex, ServerWebExchange exchange) {
        log.warn("Invalid state: {}", ex.getMessage());
        ErrorResponse error = buildError(
                HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    private ErrorResponse buildError(HttpStatus status, String error,
                                     String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}
