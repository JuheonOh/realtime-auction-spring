package com.inhatc.auction.domain.redisBid.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@RedisHash(value = "bid")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisBid {
    @Id
    private String id;

    @Indexed
    private Long auctionId;

    @Indexed
    private Long userId;

    @Indexed
    private Long bidAmount;

    private LocalDateTime bidTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}