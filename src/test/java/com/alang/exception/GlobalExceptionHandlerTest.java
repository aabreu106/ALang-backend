package com.alang.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // --- Validation errors ---

    @Test
    void handleValidationErrors_returnsBadRequest() throws NoSuchMethodException {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "password", "size must be at least 8"));

        var methodParam = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1);
        var ex = new MethodArgumentNotValidException(methodParam, bindingResult);

        var response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getDetails().get("email")).isEqualTo("must not be blank");
        assertThat(response.getBody().getDetails().get("password")).isEqualTo("size must be at least 8");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // --- Authentication errors ---

    @Test
    void handleAuthentication_withInvalidCredentials_returnsUnauthorized() {
        var ex = new InvalidCredentialsException("Invalid email or password");

        var response = handler.handleAuthentication(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    void handleAuthentication_withInvalidToken_returnsUnauthorized() {
        var ex = new InvalidTokenException("Token expired");

        var response = handler.handleAuthentication(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Token expired");
    }

    // --- Email already exists ---

    @Test
    void handleEmailAlreadyExists_returnsConflict() {
        var ex = new EmailAlreadyExistsException("test@example.com");

        var response = handler.handleEmailAlreadyExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Email already registered: test@example.com");
    }

    // --- Not found ---

    @Test
    void handleNotFound_withNoteNotFound_returns404() {
        var ex = new NoteNotFoundException("note-123");

        var response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Note not found: note-123");
    }

    @Test
    void handleNotFound_withUserNotFound_returns404() {
        var ex = new UserNotFoundException("user-456");

        var response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("User not found: user-456");
    }

    // --- Unauthorized (forbidden) ---

    @Test
    void handleUnauthorized_returnsForbidden() {
        var ex = new UnauthorizedException("You cannot access this resource");

        var response = handler.handleUnauthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("You cannot access this resource");
    }

    // --- Rate limit ---

    @Test
    void handleRateLimit_returnsTooManyRequests() {
        var ex = new RateLimitExceededException("Daily limit reached", 0);

        var response = handler.handleRateLimit(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().getMessage()).isEqualTo("Daily limit reached");
        assertThat(response.getBody().getDetails().get("remainingTokens")).isEqualTo(0L);
    }

    // --- LLM provider ---

    @Test
    void handleLLMProvider_returnsServiceUnavailable() {
        var ex = new LLMProviderException("Connection timeout");

        var response = handler.handleLLMProvider(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getMessage()).isEqualTo("AI service temporarily unavailable. Please try again.");
        assertThat(response.getBody().getDetails()).isNull();
    }

    // --- Generic fallback ---

    @Test
    void handleGenericError_returnsInternalServerError() {
        var ex = new RuntimeException("Something unexpected");

        var response = handler.handleGenericError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    // --- ErrorResponse ---

    @Test
    void errorResponse_holdsAllFields() {
        var response = new GlobalExceptionHandler.ErrorResponse("msg", null, null);

        assertThat(response.getMessage()).isEqualTo("msg");
        assertThat(response.getDetails()).isNull();
        assertThat(response.getTimestamp()).isNull();
    }
}
