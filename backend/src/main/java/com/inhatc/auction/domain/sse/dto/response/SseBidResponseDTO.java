package com.inhatc.auction.domain.sse.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SseBidResponseDTO {
    private Long id;
    private Long userId;
    private String nickname;
    private Long bidAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private Long auctionLeftTime;
}
