package com.inhatc.auction.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.auth.dto.request.AuthRequestDTO;
import com.inhatc.auction.domain.auth.dto.response.AuthResponseDTO;
import com.inhatc.auction.domain.auth.entity.Auth;
import com.inhatc.auction.domain.auth.repository.AuthRedisRepository;
import com.inhatc.auction.domain.user.dto.request.UserRequestDTO;
import com.inhatc.auction.domain.user.entity.CustomUserDetails;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.entity.UserRole;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.constant.JwtHeader;
import com.inhatc.auction.global.exception.CustomResponseStatusException;
import com.inhatc.auction.global.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String INVALID_CREDENTIALS_MESSAGE = "이메일과 비밀번호를 다시 확인해주세요.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisRepository authRedisRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 로그인
     */
    @Transactional
    public AuthResponseDTO login(@NonNull AuthRequestDTO requestDTO) {
        // 이메일과 비밀번호 확인
        User user = this.userRepository.findByEmail(requestDTO.getEmail()).orElse(null);
        String passwordHash = user == null ? DUMMY_PASSWORD_HASH : user.getPassword();
        if (!this.passwordEncoder.matches(requestDTO.getPassword(), passwordHash) || user == null) {
            throw invalidCredentials();
        }

        // 액세스 토큰 생성
        String accessToken = this.jwtTokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), user.getPassword()));

        // 리프레시 토큰 생성
        String refreshToken = this.jwtTokenProvider.generateRefreshToken(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), user.getPassword()));

        this.authRedisRepository.findAllByUserId(user.getId())
                .forEach(this.authRedisRepository::delete);

        // 리프레시 토큰 만료 시간
        Long ttl = this.jwtTokenProvider.getJwtRefreshTokenExpirationTime() / 1000;

        // 새로운 토큰 저장
        Auth auth = Auth.builder()
                .refreshTokenDigest(hashRefreshToken(refreshToken))
                .userId(user.getId())
                .ttl(ttl)
                .build();

        this.authRedisRepository.save(Objects.requireNonNull(auth));

        return AuthResponseDTO.builder()
                .tokenType(JwtHeader.TOKEN_TYPE.getValue())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 회원가입
     */
    @Transactional
    public void signup(@NonNull UserRequestDTO requestDTO) {
        // 휴대폰 번호 하이픈 제거
        requestDTO.setPhone(requestDTO.getPhone().replace("-", ""));

        // 비밀번호 암호화
        requestDTO.setPassword(passwordEncoder.encode(requestDTO.getPassword()));

        // 유저 엔티티 생성
        User user = User.builder()
                .email(requestDTO.getEmail())
                .password(requestDTO.getPassword())
                .name(requestDTO.getName())
                .phone(requestDTO.getPhone())
                .nickname(requestDTO.getNickname())
                .role(UserRole.USER)
                .build();

        // 저장
        this.userRepository.save(Objects.requireNonNull(user));
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(@NonNull String refreshToken) {
        String digest = hashRefreshToken(refreshToken);
        markRefreshTokenConsumed(digest);
        this.authRedisRepository.deleteById(digest);
    }

    public long getRefreshTokenCookieMaxAge() {
        return this.jwtTokenProvider.getJwtRefreshTokenExpirationTime() / 1000;
    }

    /**
     * Token 갱신
     */
    @Transactional
    public AuthResponseDTO refreshToken(@NonNull String refreshToken) {
        validateRefreshToken(refreshToken);

        String refreshTokenDigest = hashRefreshToken(refreshToken);
        if (!markRefreshTokenConsumed(refreshTokenDigest)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이미 사용된 리프레시 토큰입니다.");
        }
        Auth auth = this.authRedisRepository.findById(refreshTokenDigest)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "로그인이 필요합니다. (REFRESH_TOKEN)"));
        Long userId = auth.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(user), user.getPassword());
        String newAccessToken = this.jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = this.jwtTokenProvider.generateRefreshToken(authentication);

        this.authRedisRepository.deleteById(refreshTokenDigest);
        this.authRedisRepository.save(Auth.builder()
                .refreshTokenDigest(hashRefreshToken(newRefreshToken))
                .userId(user.getId())
                .ttl(this.jwtTokenProvider.getJwtRefreshTokenExpirationTime() / 1000)
                .build());

        return AuthResponseDTO.builder()
                .tokenType(JwtHeader.TOKEN_TYPE.getValue())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private void validateRefreshToken(@NonNull String refreshToken) {
        try {
            if (!this.jwtTokenProvider.validateRefreshToken(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private boolean markRefreshTokenConsumed(String refreshTokenDigest) {
        Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(
                "auth:consumed:" + refreshTokenDigest,
                "1",
                Duration.ofMillis(jwtTokenProvider.getJwtRefreshTokenExpirationTime()));
        return Boolean.TRUE.equals(created);
    }

    private CustomResponseStatusException invalidCredentials() {
        HashMap<String, String> errors = new HashMap<>();
        errors.put("credentials", INVALID_CREDENTIALS_MESSAGE);
        return new CustomResponseStatusException(HttpStatus.UNAUTHORIZED, errors);
    }
}
