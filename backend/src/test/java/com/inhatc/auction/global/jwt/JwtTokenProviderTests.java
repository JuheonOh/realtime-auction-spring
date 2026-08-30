package com.inhatc.auction.global.jwt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import com.inhatc.auction.domain.user.entity.CustomUserDetails;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.entity.UserRole;
import com.inhatc.auction.global.constant.JwtPayload;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtTokenProviderTests {
    private static final String TEST_SIGNING_KEY = "jwt-token-provider-test-signing-key-with-at-least-thirty-two-bytes";

    private JwtTokenProvider jwtTokenProvider;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", TEST_SIGNING_KEY);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtAccessTokenExpirationTime", 60_000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshTokenExpirationTime", 120_000L);
        ReflectionTestUtils.invokeMethod(jwtTokenProvider, "init");

        User user = User.builder()
                .email("user@example.test")
                .password("encoded-password")
                .name("Test User")
                .phone("01012345678")
                .nickname("tester")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 42L);
        authentication = new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), user.getPassword());
    }

    @Test
    void accessTokenValidatesOnlyAsAccessAndPreservesRequiredUserClaims() {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);

        assertDoesNotThrow(() -> jwtTokenProvider.getClaims(accessToken));
        assertEquals(42L, jwtTokenProvider.getUserIdFromToken(accessToken));
        assertEquals("Test User", jwtTokenProvider.getUserNameFromToken(accessToken));
        assertEquals("user@example.test", jwtTokenProvider.getUserEmailFromToken(accessToken));
        assertEquals("USER", jwtTokenProvider.getClaims(accessToken).get(JwtPayload.USER_ROLE.getClaims(), String.class));
        assertEquals("access", jwtTokenProvider.getClaims(accessToken).get("token_use", String.class));
        assertEquals(true, jwtTokenProvider.validateToken(accessToken));
        assertFalse(jwtTokenProvider.validateRefreshToken(accessToken));
    }

    @Test
    void refreshTokenValidatesOnlyAsRefreshAndCannotBeUsedAsAccess() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        assertEquals(true, jwtTokenProvider.validateRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.validateToken(refreshToken));
    }

    @Test
    void expiredAndWronglySignedTokensFailClosed() {
        String expiredAccessToken = Jwts.builder()
                .claim("token_use", "access")
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(TEST_SIGNING_KEY.getBytes()))
                .compact();
        String wronglySignedAccessToken = Jwts.builder()
                .claim("token_use", "access")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("different-test-signing-key-with-at-least-thirty-two-bytes".getBytes()))
                .compact();

        assertThrows(JwtException.class, () -> jwtTokenProvider.validateToken(expiredAccessToken));
        assertThrows(JwtException.class, () -> jwtTokenProvider.validateToken(wronglySignedAccessToken));
    }
}
