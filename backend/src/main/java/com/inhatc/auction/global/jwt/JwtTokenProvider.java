package com.inhatc.auction.global.jwt;

import java.util.Date;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.inhatc.auction.domain.user.entity.CustomUserDetails;
import com.inhatc.auction.global.constant.JwtHeader;
import com.inhatc.auction.global.constant.JwtPayload;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class JwtTokenProvider {
    private SecretKey jwtSecretKey;

    @Value("${spring.jwt.secret}")
    private String secret;
    @Value("${spring.jwt.access-token-expiration-time}")
    private Long jwtAccessTokenExpirationTime;
    @Value("${spring.jwt.refresh-token-expiration-time}")
    private Long jwtRefreshTokenExpirationTime;

    @PostConstruct
    protected void init() {
        this.jwtSecretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @NonNull
    public String generateToken(@NonNull Authentication authentication, long expirationTime) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        Date expiryDate = new Date(new Date().getTime() + expirationTime);
        Long userId = Objects.requireNonNull(customUserDetails.getId(), "JWT USER_ID value is missing");
        String userName = Objects.requireNonNull(customUserDetails.getUsername(), "JWT USER_NAME value is missing");
        String userEmail = Objects.requireNonNull(customUserDetails.getEmail(), "JWT USER_EMAIL value is missing");
        String userRole = Objects.requireNonNull(customUserDetails.getAuthorities().toArray()[0], "JWT USER_ROLE value is missing")
                .toString();

        String token = Jwts.builder()
                .signWith(Objects.requireNonNull(jwtSecretKey, "JWT secret key is not initialized")) // 암호화 알고리즘, secret값 세팅
                .header() // 토큰의 헤더 설정
                .add("typ", JwtHeader.TOKEN_TYPE.getValue()) // 토큰의 타입
                .and() // 헤더 설정 종료
                .claim(JwtPayload.USER_ID.getClaims(), userId)
                .claim(JwtPayload.USER_NAME.getClaims(), userName)
                .claim(JwtPayload.USER_EMAIL.getClaims(), userEmail)
                .claim(JwtPayload.USER_ROLE.getClaims(), userRole)
                .issuedAt(new Date()) // 토큰 발급 시간
                .expiration(expiryDate) // 토큰 만료 시간
                .compact(); // 토큰 생성
        return Objects.requireNonNull(token, "JWT token creation failed");
    }

    @NonNull
    public String generateAccessToken(@NonNull Authentication authentication) {
        return generateToken(authentication, jwtAccessTokenExpirationTime);
    }

    @NonNull
    public String generateRefreshToken(@NonNull Authentication authentication) {
        return generateToken(authentication, jwtRefreshTokenExpirationTime);
    }

    @Nullable
    public String getTokenFromRequest(@NonNull HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtHeader.TOKEN_HEADER.getValue());

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtHeader.TOKEN_PREFIX.getValue())) {
            return bearerToken.substring(JwtHeader.TOKEN_PREFIX.getValue().length());
        }

        return null;
    }

    @NonNull
    public Long getUserIdFromToken(@NonNull String token) {
        Long userId = getClaims(token).get(JwtPayload.USER_ID.getClaims(), Long.class);
        return Objects.requireNonNull(userId, "JWT USER_ID claim is missing");
    }

    @NonNull
    public String getUserNameFromToken(@NonNull String token) {
        String userName = getClaims(token).get(JwtPayload.USER_NAME.getClaims(), String.class);
        return Objects.requireNonNull(userName, "JWT USER_NAME claim is missing");
    }

    @NonNull
    public String getUserEmailFromToken(@NonNull String token) {
        String userEmail = getClaims(token).get(JwtPayload.USER_EMAIL.getClaims(), String.class);
        return Objects.requireNonNull(userEmail, "JWT USER_EMAIL claim is missing");
    }

    @NonNull
    public Date getExpirationFromToken(@NonNull String token) {
        return Objects.requireNonNull(getClaims(token).getExpiration(), "JWT expiration is missing");
    }

    public long getJwtRefreshTokenExpirationTime() {
        return jwtRefreshTokenExpirationTime;
    }

    @NonNull
    public Claims getClaims(@NonNull String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Objects.requireNonNull(claims, "JWT claims are missing");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.info("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.info("잘못된 JWT 서명: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 비어있음: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(@NonNull String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.info("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 비어있음: {}", e.getMessage());
            throw e;
        }
    }
}
