package com.alang.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExceptionsTest {

    // --- EmailAlreadyExistsException ---

    @Test
    void emailAlreadyExists_formatsMessageWithEmail() {
        var ex = new EmailAlreadyExistsException("test@example.com");

        assertThat(ex.getMessage()).isEqualTo("Email already registered: test@example.com");
    }

    @Test
    void emailAlreadyExists_isRuntimeException() {
        var ex = new EmailAlreadyExistsException("a@b.com");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    // --- InvalidCredentialsException ---

    @Test
    void invalidCredentials_preservesMessage() {
        var ex = new InvalidCredentialsException("Bad password");

        assertThat(ex.getMessage()).isEqualTo("Bad password");
    }

    // --- InvalidTokenException ---

    @Test
    void invalidToken_preservesMessage() {
        var ex = new InvalidTokenException("Token expired");

        assertThat(ex.getMessage()).isEqualTo("Token expired");
    }

    // --- NoteNotFoundException ---

    @Test
    void noteNotFound_formatsMessageWithId() {
        var ex = new NoteNotFoundException("note-123");

        assertThat(ex.getMessage()).isEqualTo("Note not found: note-123");
    }

    // --- UserNotFoundException ---

    @Test
    void userNotFound_formatsMessageWithId() {
        var ex = new UserNotFoundException("user-456");

        assertThat(ex.getMessage()).isEqualTo("User not found: user-456");
    }

    // --- UnauthorizedException ---

    @Test
    void unauthorized_preservesMessage() {
        var ex = new UnauthorizedException("Access denied");

        assertThat(ex.getMessage()).isEqualTo("Access denied");
    }

    // --- RateLimitExceededException ---

    @Test
    void rateLimitExceeded_preservesMessageAndTokens() {
        var ex = new RateLimitExceededException("Rate limit hit", 500);

        assertThat(ex.getMessage()).isEqualTo("Rate limit hit");
        assertThat(ex.getRemainingTokens()).isEqualTo(500);
    }

    @Test
    void rateLimitExceeded_handlesZeroRemainingTokens() {
        var ex = new RateLimitExceededException("No tokens left", 0);

        assertThat(ex.getRemainingTokens()).isEqualTo(0);
    }

    // --- LLMProviderException ---

    @Test
    void llmProvider_preservesMessage() {
        var ex = new LLMProviderException("Timeout");

        assertThat(ex.getMessage()).isEqualTo("Timeout");
    }

    @Test
    void llmProvider_preservesCause() {
        var cause = new RuntimeException("connection refused");
        var ex = new LLMProviderException("LLM call failed", cause);

        assertThat(ex.getMessage()).isEqualTo("LLM call failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
