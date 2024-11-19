package com.inhatc.auction.domain.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "auth")
public class Auth {
    @Id
    private String refreshToken;
    private Long userId;

    @TimeToLive
    private long ttl;
}