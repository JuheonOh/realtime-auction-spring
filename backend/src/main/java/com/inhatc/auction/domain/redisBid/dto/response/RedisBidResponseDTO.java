package com.inhatc.auction.domain.redisBid.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisBidResponseDTO {
    private Long auctionId;
    private Long userId;
    private Long bidAmount;
    private LocalDateTime bidTime;
}
