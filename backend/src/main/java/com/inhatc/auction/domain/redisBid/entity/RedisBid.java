package com.inhatc.auction.domain.redisBid.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@RedisHash(value = "auction_bid")
@Getter
@NoArgsConstructor
public class RedisBid {
    @Id
    private String id;

    @Indexed
    private Long auctionId;

    @Indexed
    private Long userId;

    private Long bidAmount;
    private LocalDateTime bidTime;

    @Builder
    public RedisBid(Long auctionId, Long userId, Long bidAmount) {
        this.id = String.format("auction:%d:bid:%s", auctionId, UUID.randomUUID().toString());
        this.auctionId = auctionId;
        this.userId = userId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }
}