package com.inhatc.auction.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.auth.dto.request.AuthRequestDTO;
import com.inhatc.auction.domain.auth.dto.response.AuthResponseDTO;
import com.inhatc.auction.domain.auth.entity.Auth;
import com.inhatc.auction.domain.auth.repository.AuthRedisRepository;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.entity.UserRole;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.exception.CustomResponseStatusException;
import com.inhatc.auction.global.jwt.JwtTokenProvider;

class AuthServiceTests {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthRedisRepository authRedisRepository;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authRedisRepository = mock(AuthRedisRepository.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(true);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, authRedisRepository,
                stringRedisTemplate);
    }

    @Test
    void unknownUserUsesDummyBcryptComparisonAndReturnsSameInvalidCredentialsShape() {
        PasswordEncoder bcryptEncoder = spy(new BCryptPasswordEncoder());
        authService = new AuthService(userRepository, bcryptEncoder, jwtTokenProvider, authRedisRepository,
                stringRedisTemplate);
        AuthRequestDTO request = loginRequest("missing@example.test", "wrong-password");
        User existingUser = user(7L, "existing@example.test", new BCryptPasswordEncoder().encode("correct-password"));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(existingUser.getEmail())).thenReturn(Optional.of(existingUser));

        CustomResponseStatusException unknownUserError = assertThrows(CustomResponseStatusException.class,
                () -> authService.login(request));
        CustomResponseStatusException wrongPasswordError = assertThrows(CustomResponseStatusException.class,
                () -> authService.login(loginRequest(existingUser.getEmail(), "wrong-password")));

        ArgumentCaptor<String> comparedHashes = ArgumentCaptor.forClass(String.class);
        verify(bcryptEncoder, times(2)).matches(eq("wrong-password"), comparedHashes.capture());
        assertTrue(comparedHashes.getAllValues().contains("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"));
        assertEquals(HttpStatus.UNAUTHORIZED, unknownUserError.getStatusCode());
        assertEquals(wrongPasswordError.getStatusCode(), unknownUserError.getStatusCode());
        assertEquals(wrongPasswordError.getReason(), unknownUserError.getReason());
        assertEquals(wrongPasswordError.getErrors(), unknownUserError.getErrors());
    }

    @Test
    void loginStoresOnlyRefreshDigestAndRevokesPriorSessionsFoundByUserIndex() {
        User user = user(11L, "user@example.test", "encoded-password");
        Auth priorSession = Auth.builder().refreshTokenDigest("prior-digest").userId(user.getId()).ttl(30).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getJwtRefreshTokenExpirationTime()).thenReturn(120_000L);
        when(authRedisRepository.findAllByUserId(user.getId())).thenReturn(List.of(priorSession));

        authService.login(loginRequest(user.getEmail(), "password"));

        ArgumentCaptor<Auth> savedAuth = ArgumentCaptor.forClass(Auth.class);
        verify(authRedisRepository).findAllByUserId(user.getId());
        verify(authRedisRepository).delete(priorSession);
        verify(authRedisRepository).save(savedAuth.capture());
        assertEquals(sha256("new-refresh-token"), savedAuth.getValue().getRefreshTokenDigest());
        assertNotEquals("new-refresh-token", savedAuth.getValue().getRefreshTokenDigest());
        assertEquals(user.getId(), savedAuth.getValue().getUserId());
        assertEquals(120L, savedAuth.getValue().getTtl());
    }

    @Test
    void refreshDeletesOldDigestAndSavesDigestForRotatedRefreshToken() {
        User user = user(12L, "refresh@example.test", "encoded-password");
        String oldRefreshToken = "old-refresh-token";
        Auth existingAuth = Auth.builder().refreshTokenDigest(sha256(oldRefreshToken)).userId(user.getId()).ttl(120).build();
        when(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(authRedisRepository.findById(sha256(oldRefreshToken))).thenReturn(Optional.of(existingAuth));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("rotated-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("rotated-refresh-token");
        when(jwtTokenProvider.getJwtRefreshTokenExpirationTime()).thenReturn(180_000L);

        AuthResponseDTO response = authService.refreshToken(oldRefreshToken);

        ArgumentCaptor<Auth> savedAuth = ArgumentCaptor.forClass(Auth.class);
        verify(authRedisRepository).deleteById(sha256(oldRefreshToken));
        verify(authRedisRepository).save(savedAuth.capture());
        assertEquals("rotated-refresh-token", response.getRefreshToken());
        assertEquals(sha256("rotated-refresh-token"), savedAuth.getValue().getRefreshTokenDigest());
        assertNotEquals(oldRefreshToken, savedAuth.getValue().getRefreshTokenDigest());
        assertEquals(user.getId(), savedAuth.getValue().getUserId());
        assertEquals(180L, savedAuth.getValue().getTtl());
    }

    @Test
    void logoutHashesRefreshTokenBeforeRepositoryLookup() {
        authService.logout("plain-refresh-token");

        verify(authRedisRepository).deleteById(sha256("plain-refresh-token"));
    }

    @Test
    void refreshRejectsAReplayBeforeReadingTheStoredSession() {
        String token = "already-consumed-refresh-token";
        when(jwtTokenProvider.validateRefreshToken(token)).thenReturn(true);
        when(jwtTokenProvider.getJwtRefreshTokenExpirationTime()).thenReturn(180_000L);
        when(valueOperations.setIfAbsent(any(String.class), eq("1"), any(Duration.class))).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(token));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(authRedisRepository, never()).findById(any(String.class));
    }

    private AuthRequestDTO loginRequest(String email, String password) {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User user(Long id, String email, String password) {
        User user = User.builder()
                .email(email)
                .password(password)
                .name("Test User")
                .phone("01012345678")
                .nickname("tester-" + id)
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
