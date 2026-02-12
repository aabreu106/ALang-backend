package com.alang.exception;

/**
 * Exception thrown when LLM API call fails.
 *
 * This could be:
 * - Network timeout
 * - API rate limit
 * - Invalid response format
 * - Provider downtime
 */
public class LLMProviderException extends RuntimeException {
    public LLMProviderException(String message) {
        super(message);
    }

    public LLMProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
