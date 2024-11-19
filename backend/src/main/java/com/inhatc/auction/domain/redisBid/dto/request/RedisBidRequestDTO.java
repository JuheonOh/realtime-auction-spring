package com.inhatc.auction.domain.redisBid.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisBidRequestDTO {
    private Long auctionId;
    private Long userId;
    private Long bidAmount;
}
