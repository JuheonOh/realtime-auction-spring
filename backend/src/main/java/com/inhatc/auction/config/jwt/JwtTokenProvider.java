package com.inhatc.auction.config.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.inhatc.auction.config.SecurityConstants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class JwtTokenProvider {
    private SecretKey jwtSecretKey;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;
    @Value("${jwt.refreshTokenExpirationTime}")
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
                .add("typ", SecurityConstants.TOKEN_TYPE) // 토큰의 타입
                .and() // 헤더 설정 종료
                .claim("user-id", customUserDetails.getId())
                .claim("user-name", customUserDetails.getUsername())
                .claim("user-email", customUserDetails.getEmail())
                .claim("user-role", customUserDetails.getAuthorities().toArray()[0].toString())
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

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get("user-id", Long.class);
    }

    public String getUserNameFromToken(String token) {
        return getClaims(token).get("user-name", String.class);
    }

    public String getUserEmailFromToken(String token) {
        return getClaims(token).get("user-email", String.class);
    }

    public Date getExpirationFromToken(String token) {
        return getClaims(token).getExpiration();
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
