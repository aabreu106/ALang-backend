package com.alang.exception;

public class RateLimitExceededException extends RuntimeException {
    private final long remainingTokens;

    public RateLimitExceededException(String message, long remainingTokens) {
        super(message);
        this.remainingTokens = remainingTokens;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }
}
