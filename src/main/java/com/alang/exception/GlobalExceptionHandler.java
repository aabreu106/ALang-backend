package com.alang.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler.
 *
 * Catches exceptions thrown from controllers and returns proper HTTP responses.
 *
 * TODO: Add logging for all exceptions
 * TODO: Add monitoring/alerting for errors
 * TODO: Add Sentry or similar error tracking
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (@Valid annotations).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse response = new ErrorResponse(
            "Validation failed",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle authentication errors.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        ErrorResponse response = new ErrorResponse(
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle resource not found errors.
     */
    @ExceptionHandler({NoteNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        ErrorResponse response = new ErrorResponse(
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle unauthorized access (user trying to access another user's data).
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponse response = new ErrorResponse(
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle rate limit errors.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        ErrorResponse response = new ErrorResponse(
            ex.getMessage(),
            Map.of("remainingTokens", ex.getRemainingTokens()),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Handle LLM provider errors.
     */
    @ExceptionHandler(LLMProviderException.class)
    public ResponseEntity<ErrorResponse> handleLLMProvider(LLMProviderException ex) {
        ErrorResponse response = new ErrorResponse(
            "AI service temporarily unavailable. Please try again.",
            null,
            LocalDateTime.now()
        );
        // TODO: Log full exception details for debugging
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle all other exceptions (fallback).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        // TODO: Log this exception
        ErrorResponse response = new ErrorResponse(
            "An unexpected error occurred",
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Error response DTO.
     */
    public static class ErrorResponse {
        private String message;
        private Map<String, ?> details;
        private LocalDateTime timestamp;

        public ErrorResponse(String message, Map<String, ?> details, LocalDateTime timestamp) {
            this.message = message;
            this.details = details;
            this.timestamp = timestamp;
        }

        // Getters
        public String getMessage() { return message; }
        public Map<String, ?> getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
