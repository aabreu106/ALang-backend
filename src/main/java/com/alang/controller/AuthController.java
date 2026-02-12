package com.alang.controller;

import com.alang.dto.auth.AuthResponse;
import com.alang.dto.auth.LoginRequest;
import com.alang.dto.auth.SignupRequest;
import com.alang.dto.auth.UserResponse;
import com.alang.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 *
 * ARCHITECTURAL PRINCIPLE:
 * This controller is THIN. It does:
 * - Request validation (via @Valid)
 * - Calling AuthService
 * - Returning HTTP responses
 *
 * It does NOT:
 * - Hash passwords (done in AuthService)
 * - Generate JWT tokens (done in AuthService)
 * - Validate business logic (done in AuthService)
 *
 * TODO: Inject AuthService
 * TODO: Add proper error handling (@ExceptionHandler)
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    // TODO: Inject AuthService
    // private final AuthService authService;

    /**
     * POST /auth/signup
     * Register a new user.
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        // TODO: Call authService.signup(request)
        // TODO: Return 201 Created with AuthResponse
        throw new UnsupportedOperationException("TODO: Implement signup");
    }

    /**
     * POST /auth/login
     * Authenticate a user.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // TODO: Call authService.login(request)
        // TODO: Return 200 OK with AuthResponse
        throw new UnsupportedOperationException("TODO: Implement login");
    }

    /**
     * GET /auth/me
     * Get current authenticated user profile.
     *
     * @AuthenticationPrincipal extracts user ID from JWT token
     * (configured in SecurityConfig)
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal String userId) {
        // TODO: Call authService.getCurrentUser(userId)
        // TODO: Return 200 OK with UserResponse
        throw new UnsupportedOperationException("TODO: Implement get current user");
    }
}
