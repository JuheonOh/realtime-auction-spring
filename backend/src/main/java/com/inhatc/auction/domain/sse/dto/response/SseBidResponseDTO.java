package com.inhatc.auction.domain.sse.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseBidResponseDTO {
    private Long id;
    private Long userId;
    private String nickname;
    private Long bidAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bidTime;

    private Long auctionLeftTime;
}
