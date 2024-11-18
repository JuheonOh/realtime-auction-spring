package com.inhatc.auction.domain.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@RedisHash(value = "auth")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auth {
    @Id
    private Long id; // userId를 key로 사용

    private String tokenType;

    @Indexed
    private String accessToken;

    @Indexed
    private String refreshToken;

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
