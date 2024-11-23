package com.inhatc.auction.global.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
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

    public String generateToken(Authentication authentication, long expirationTime) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        Date expiryDate = new Date(new Date().getTime() + expirationTime);

        return Jwts.builder()
                .signWith(jwtSecretKey) // 암호화 알고리즘, secret값 세팅
                .header() // 토큰의 헤더 설정
                .add("typ", JwtHeader.TOKEN_TYPE.getValue()) // 토큰의 타입
                .and() // 헤더 설정 종료
                .claim(JwtPayload.USER_ID.getClaims(), customUserDetails.getId())
                .claim(JwtPayload.USER_NAME.getClaims(), customUserDetails.getUsername())
                .claim(JwtPayload.USER_EMAIL.getClaims(), customUserDetails.getEmail())
                .claim(JwtPayload.USER_ROLE.getClaims(), customUserDetails.getAuthorities().toArray()[0].toString())
                .issuedAt(new Date()) // 토큰 발급 시간
                .expiration(expiryDate) // 토큰 만료 시간
                .compact(); // 토큰 생성
    }

    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtAccessTokenExpirationTime);
    }

    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jwtRefreshTokenExpirationTime);
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtHeader.TOKEN_HEADER.getValue());

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtHeader.TOKEN_PREFIX.getValue())) {
            return bearerToken.substring(JwtHeader.TOKEN_PREFIX.getValue().length());
        }

        return null;
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get(JwtPayload.USER_ID.getClaims(), Long.class);
    }

    public String getUserNameFromToken(String token) {
        return getClaims(token).get(JwtPayload.USER_NAME.getClaims(), String.class);
    }

    public String getUserEmailFromToken(String token) {
        return getClaims(token).get(JwtPayload.USER_EMAIL.getClaims(), String.class);
    }

    public Date getExpirationFromToken(String token) {
        return getClaims(token).getExpiration();
    }

    public long getJwtRefreshTokenExpirationTime() {
        return jwtRefreshTokenExpirationTime;
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
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

    public boolean validateToken(String token) {
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
