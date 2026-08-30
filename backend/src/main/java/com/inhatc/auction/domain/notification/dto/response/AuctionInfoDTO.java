package com.inhatc.auction.domain.notification.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class AuctionInfoDTO {
    private Long id;
    private String title;
    private Long currentPrice;
    private Long successfulPrice;
    private String filePath;
    private String fileName;
    private LocalDateTime auctionEndTime;
}
