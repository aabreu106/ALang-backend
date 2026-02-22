package com.alang.service.impl;

import com.alang.config.JwtTokenProvider;
import com.alang.dto.auth.AuthResponse;
import com.alang.dto.auth.LoginRequest;
import com.alang.dto.auth.SignupRequest;
import com.alang.dto.auth.UserResponse;
import com.alang.entity.User;
import com.alang.exception.EmailAlreadyExistsException;
import com.alang.exception.InvalidCredentialsException;
import com.alang.exception.InvalidTokenException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.LanguageRepository;
import com.alang.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserServiceImpl userService;

    // --- signup ---

    @Test
    void signup_createsUserAndReturnsToken() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setDisplayName("Test User");
        request.setAppLanguageCode("en");
        request.setTargetLanguageCodes(List.of("ja"));

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });
        when(jwtTokenProvider.generateToken("user-1")).thenReturn("jwt-token");

        AuthResponse response = userService.signup(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getDisplayName()).isEqualTo("Test User");
    }

    @Test
    void signup_hashesPassword() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("myPassword");
        request.setDisplayName("User");
        request.setAppLanguageCode("en");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("myPassword")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("id");
            return u;
        });
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");

        userService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void signup_defaultsTargetLanguagesToEmptyList() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setDisplayName("User");
        request.setAppLanguageCode("en");
        request.setTargetLanguageCodes(null);

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("id");
            return u;
        });
        when(jwtTokenProvider.generateToken(any())).thenReturn("token");

        userService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetLanguageCodes()).isEmpty();
    }

    @Test
    void signup_throwsWhenEmailAlreadyExists() {
        SignupRequest request = new SignupRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setDisplayName("User");
        request.setAppLanguageCode("en");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_returnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setDisplayName("Test User");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken("user-1")).thenReturn("jwt-token");

        AuthResponse response = userService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo("user-1");
    }

    @Test
    void login_throwsWhenEmailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_throwsWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        User user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    // --- getCurrentUser ---

    @Test
    void getCurrentUser_returnsUserResponse() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setAppLanguageCode("en");
        user.setTargetLanguageCodes(List.of("ja", "es"));

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser("user-1");

        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getDisplayName()).isEqualTo("Test User");
        assertThat(response.getAppLanguage()).isEqualTo("en");
        assertThat(response.getTargetLanguages()).containsExactly("ja", "es");
    }

    @Test
    void getCurrentUser_throwsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- addTargetLanguage ---

    @Test
    void addTargetLanguage_addsLanguageAndReturnsUpdatedUser() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setAppLanguageCode("en");
        user.setTargetLanguageCodes(new ArrayList<>(List.of("es")));

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(languageRepository.existsById("ja")).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.addTargetLanguage("user-1", "ja");

        assertThat(response.getTargetLanguages()).containsExactlyInAnyOrder("es", "ja");
        verify(userRepository).save(user);
    }

    @Test
    void addTargetLanguage_isIdempotentWhenLanguageAlreadyPresent() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setAppLanguageCode("en");
        user.setTargetLanguageCodes(new ArrayList<>(List.of("ja")));

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(languageRepository.existsById("ja")).thenReturn(true);

        UserResponse response = userService.addTargetLanguage("user-1", "ja");

        assertThat(response.getTargetLanguages()).containsExactly("ja");
        verify(userRepository, never()).save(any());
    }

    @Test
    void addTargetLanguage_throwsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addTargetLanguage("missing", "ja"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void addTargetLanguage_throwsWhenLanguageCodeNotRecognized() {
        User user = new User();
        user.setId("user-1");
        user.setTargetLanguageCodes(new ArrayList<>());

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(languageRepository.existsById("xx")).thenReturn(false);

        assertThatThrownBy(() -> userService.addTargetLanguage("user-1", "xx"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- generateToken ---

    @Test
    void generateToken_delegatesToJwtTokenProvider() {
        when(jwtTokenProvider.generateToken("user-1")).thenReturn("generated-token");

        String token = userService.generateToken("user-1");

        assertThat(token).isEqualTo("generated-token");
    }

    // --- validateToken ---

    @Test
    void validateToken_returnsUserIdForValidToken() {
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn("user-1");

        String userId = userService.validateToken("valid-token");

        assertThat(userId).isEqualTo("user-1");
    }

    @Test
    void validateToken_throwsForInvalidToken() {
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> userService.validateToken("bad-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired token");
    }
}
