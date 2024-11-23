package com.inhatc.auction.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtHeader {
    TOKEN_HEADER("Authorization"), // JWT 토큰을 담을 HTTP 요청 헤더 이름
    TOKEN_PREFIX("Bearer "), // 헤더의 접두사
    TOKEN_TYPE("JWT"); // 토큰 타입

    private final String value;
}