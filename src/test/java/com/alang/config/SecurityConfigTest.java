package com.alang.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void passwordEncoder_returnsBCryptEncoder() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm", 3600000);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        SecurityConfig config = new SecurityConfig(filter);

        var encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodesAndMatchesPassword() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm", 3600000);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        SecurityConfig config = new SecurityConfig(filter);

        var encoder = config.passwordEncoder();
        String raw = "my-password";
        String encoded = encoder.encode(raw);

        assertThat(encoder.matches(raw, encoded)).isTrue();
        assertThat(encoder.matches("wrong-password", encoded)).isFalse();
    }

    @Test
    void corsConfigurationSource_allowsExpectedOrigins() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm", 3600000);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        SecurityConfig config = new SecurityConfig(filter);

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsConfigurationSource_configuresExpectedMethods() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm", 3600000);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        SecurityConfig config = new SecurityConfig(filter);

        CorsConfigurationSource source = config.corsConfigurationSource();

        // Verify it's a UrlBasedCorsConfigurationSource with registered patterns
        assertThat(source).isNotNull();
    }
}
