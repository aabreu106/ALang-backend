package com.alang.service;

import com.alang.dto.auth.AuthResponse;
import com.alang.dto.auth.LoginRequest;
import com.alang.dto.auth.SignupRequest;
import com.alang.dto.auth.UserResponse;

/**
 * Authentication and user management service.
 *
 * RESPONSIBILITIES:
 * - User registration
 * - User login (JWT generation)
 * - Password management
 * - User profile retrieval
 *
 * TODO: Implement JWT token generation/validation
 * TODO: Implement password hashing (BCrypt)
 * TODO: Implement email verification
 * TODO: Implement password reset flow
 */
public interface UserService {

    /**
     * Register a new user.
     *
     * FLOW:
     * 1. Validate email not already registered
     * 2. Hash password (BCrypt)
     * 3. Create User entity
     * 4. Generate JWT token
     * 5. Return AuthResponse with token
     *
     * TODO: Send verification email
     * TODO: Validate password strength
     *
     * @param request Signup request (email, password, displayName)
     * @return Auth response with JWT token
     * @throws EmailAlreadyExistsException if email already registered
     */
    AuthResponse signup(SignupRequest request);

    /**
     * Authenticate a user.
     *
     * FLOW:
     * 1. Find user by email
     * 2. Verify password (BCrypt.checkpw)
     * 3. Generate JWT token
     * 4. Return AuthResponse with token
     *
     * @param request Login request (email, password)
     * @return Auth response with JWT token
     * @throws InvalidCredentialsException if email/password wrong
     */
    AuthResponse login(LoginRequest request);

    /**
     * Get current user profile.
     *
     * @param userId Authenticated user ID (from JWT)
     * @return User profile
     */
    UserResponse getCurrentUser(String userId);

    /**
     * Add a target language to the current user's profile.
     *
     * @param userId       Authenticated user ID (from JWT)
     * @param languageCode ISO 639-1 language code to add (e.g. "ja", "es")
     * @return Updated user profile
     * @throws UserNotFoundException if user not found
     * @throws ResponseStatusException (404) if language code not recognized
     */
    UserResponse addTargetLanguage(String userId, String languageCode);

    /**
     * Generate JWT token for a user.
     *
     * Token should include:
     * - User ID
     * - Email
     * - Expiration (e.g., 24 hours)
     *
     * TODO: Implement using io.jsonwebtoken (already in pom.xml)
     *
     * @param userId User ID
     * @return JWT token string
     */
    String generateToken(String userId);

    /**
     * Validate JWT token and extract user ID.
     *
     * TODO: Implement JWT validation
     * TODO: Handle expired tokens
     *
     * @param token JWT token
     * @return User ID from token
     * @throws InvalidTokenException if token invalid or expired
     */
    String validateToken(String token);
}
