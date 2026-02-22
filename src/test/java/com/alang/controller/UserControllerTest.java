package com.alang.controller;

import com.alang.dto.auth.AddLanguageRequest;
import com.alang.dto.auth.AuthResponse;
import com.alang.dto.auth.LoginRequest;
import com.alang.dto.auth.SignupRequest;
import com.alang.dto.auth.UserResponse;
import com.alang.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void signup_returnsCreatedStatus() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setDisplayName("Test User");
        request.setAppLanguageCode("en");
        request.setTargetLanguageCodes(List.of("ja"));

        AuthResponse authResponse = new AuthResponse("token-123", "user-1", "test@example.com", "Test User");
        when(userService.signup(request)).thenReturn(authResponse);

        var response = userController.signup(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(authResponse);
        verify(userService).signup(request);
    }

    @Test
    void login_returnsOkStatus() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse("token-456", "user-1", "test@example.com", "Test User");
        when(userService.login(request)).thenReturn(authResponse);

        var response = userController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(authResponse);
        verify(userService).login(request);
    }

    @Test
    void getCurrentUser_returnsOkWithUserResponse() {
        String userId = "user-1";
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(userId);
        userResponse.setEmail("test@example.com");
        userResponse.setDisplayName("Test User");
        userResponse.setAppLanguage("en");
        userResponse.setTargetLanguages(List.of("ja"));

        when(userService.getCurrentUser(userId)).thenReturn(userResponse);

        var response = userController.getCurrentUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponse);
        verify(userService).getCurrentUser(userId);
    }

    @Test
    void addTargetLanguage_returnsOkWithUpdatedUserResponse() {
        String userId = "user-1";
        AddLanguageRequest request = new AddLanguageRequest();
        request.setLanguageCode("ja");

        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(userId);
        userResponse.setEmail("test@example.com");
        userResponse.setDisplayName("Test User");
        userResponse.setAppLanguage("en");
        userResponse.setTargetLanguages(List.of("ja"));

        when(userService.addTargetLanguage(userId, "ja")).thenReturn(userResponse);

        var response = userController.addTargetLanguage(userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponse);
        verify(userService).addTargetLanguage(userId, "ja");
    }

    @Test
    void signup_delegatesToUserService() {
        SignupRequest request = new SignupRequest();
        request.setEmail("new@example.com");
        request.setPassword("securepass");
        request.setDisplayName("New User");
        request.setAppLanguageCode("ja");

        when(userService.signup(request)).thenReturn(
                new AuthResponse("token", "user-2", "new@example.com", "New User"));

        userController.signup(request);

        verify(userService, times(1)).signup(request);
        verifyNoMoreInteractions(userService);
    }
}
