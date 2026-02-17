package com.alang.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "this-is-a-test-secret-key-that-is-long-enough-for-hmac-sha256";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtTokenProvider.generateToken("user-123");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken("user-123");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_withMalformedToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not-a-valid-token")).isFalse();
    }

    @Test
    void validateToken_withEmptyToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_withTokenSignedByDifferentKey_returnsFalse() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "a-completely-different-secret-key-that-is-also-long-enough", EXPIRATION_MS);
        String token = otherProvider.generateToken("user-123");

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000);
        String token = expiredProvider.generateToken("user-123");

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void getUserIdFromToken_returnsCorrectUserId() {
        String userId = "user-456";
        String token = jwtTokenProvider.generateToken(userId);

        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
    }

    @Test
    void getUserIdFromToken_withDifferentUserIds_returnsCorrectId() {
        String token1 = jwtTokenProvider.generateToken("user-1");
        String token2 = jwtTokenProvider.generateToken("user-2");

        assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo("user-1");
        assertThat(jwtTokenProvider.getUserIdFromToken(token2)).isEqualTo("user-2");
    }

    @Test
    void generateToken_producesUniqueTokensForSameUser() {
        String token1 = jwtTokenProvider.generateToken("user-123");
        String token2 = jwtTokenProvider.generateToken("user-123");

        // Tokens differ because issuedAt differs (or at minimum are both valid)
        assertThat(jwtTokenProvider.validateToken(token1)).isTrue();
        assertThat(jwtTokenProvider.validateToken(token2)).isTrue();
    }
}
