package com.inhatc.auction.domain.notification.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuctionInfoDTO {
    private Long id;
    private String title;
    private Long currentPrice;
    private Long successfulPrice;
    private String filePath;
    private String fileName;
    private LocalDateTime auctionEndTime;
}
