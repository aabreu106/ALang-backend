package com.alang.controller;

import com.alang.dto.auth.AddLanguageRequest;
import com.alang.dto.auth.AuthResponse;
import com.alang.dto.auth.LoginRequest;
import com.alang.dto.auth.SignupRequest;
import com.alang.dto.auth.UserResponse;
import com.alang.service.UserService;
import jakarta.validation.Valid;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }

    @PostMapping("/me/languages")
    public ResponseEntity<UserResponse> addTargetLanguage(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddLanguageRequest request) {
        return ResponseEntity.ok(userService.addTargetLanguage(userId, request.getLanguageCode()));
    }
}
