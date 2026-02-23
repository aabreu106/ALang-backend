package com.alang.service.impl;

import com.alang.config.JwtTokenProvider;
import com.alang.dto.auth.AuthResponse;
import com.alang.dto.auth.LoginRequest;
import com.alang.dto.auth.SignupRequest;
import com.alang.dto.auth.UserResponse;
import com.alang.entity.User;
import com.alang.exception.EmailAlreadyExistsException;
import com.alang.exception.InvalidCredentialsException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.LanguageRepository;
import com.alang.repository.UserRepository;
import com.alang.service.UserService;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final Logger LOGGER = Logger.getLogger(UserServiceImpl.class.getName());

    @Override
    public AuthResponse signup(SignupRequest request) {
        request.setEmail(request.getEmail().toLowerCase());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        
        LOGGER.info("Registering new user with username: " + request.getDisplayName());
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setAppLanguageCode(request.getAppLanguageCode());
        user.setTargetLanguageCodes(
                request.getTargetLanguageCodes() != null ? request.getTargetLanguageCodes() : new ArrayList<>()
        );

        user = userRepository.save(user);
        LOGGER.info("New user registered: " + user.getDisplayName());
        String token = jwtTokenProvider.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        request.setEmail(request.getEmail().toLowerCase());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName());
    }

    @Override
    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserResponse response = new UserResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setAppLanguage(user.getAppLanguageCode());
        response.setTargetLanguages(user.getTargetLanguageCodes());
        return response;
    }

    @Override
    public UserResponse addTargetLanguage(String userId, String languageCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!languageRepository.existsById(languageCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown language code: " + languageCode);
        }

        if (!user.getTargetLanguageCodes().contains(languageCode)) {
            user.getTargetLanguageCodes().add(languageCode);
            userRepository.save(user);
        }

        UserResponse response = new UserResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setAppLanguage(user.getAppLanguageCode());
        response.setTargetLanguages(user.getTargetLanguageCodes());
        return response;
    }

    @Override
    public String generateToken(String userId) {
        return jwtTokenProvider.generateToken(userId);
    }

    @Override
    public String validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new com.alang.exception.InvalidTokenException("Invalid or expired token");
        }
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
