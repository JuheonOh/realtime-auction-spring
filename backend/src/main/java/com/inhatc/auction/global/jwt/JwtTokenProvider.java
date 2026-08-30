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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtTokenProvider {
    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String ACCESS_TOKEN_USE = "access";
    private static final String REFRESH_TOKEN_USE = "refresh";

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
    private String generateToken(@NonNull Authentication authentication, long expirationTime, @NonNull String tokenUse) {
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
                .claim(TOKEN_USE_CLAIM, tokenUse)
                .issuedAt(new Date()) // 토큰 발급 시간
                .expiration(expiryDate) // 토큰 만료 시간
                .compact(); // 토큰 생성
        return Objects.requireNonNull(token, "JWT token creation failed");
    }

    @NonNull
    public String generateAccessToken(@NonNull Authentication authentication) {
        return generateToken(authentication, jwtAccessTokenExpirationTime, ACCESS_TOKEN_USE);
    }

    @NonNull
    public String generateRefreshToken(@NonNull Authentication authentication) {
        return generateToken(authentication, jwtRefreshTokenExpirationTime, REFRESH_TOKEN_USE);
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
        Claims claims = Jwts.parser()
                .verifyWith(Objects.requireNonNull(jwtSecretKey, "JWT secret key is not initialized"))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Objects.requireNonNull(claims, "JWT claims are missing");
    }

    public boolean validateToken(@NonNull String token) {
        return hasTokenUse(token, ACCESS_TOKEN_USE);
    }

    public boolean validateRefreshToken(@NonNull String token) {
        return hasTokenUse(token, REFRESH_TOKEN_USE);
    }

    private boolean hasTokenUse(@NonNull String token, @NonNull String expectedTokenUse) {
        return expectedTokenUse.equals(getClaims(token).get(TOKEN_USE_CLAIM, String.class));
    }
}
