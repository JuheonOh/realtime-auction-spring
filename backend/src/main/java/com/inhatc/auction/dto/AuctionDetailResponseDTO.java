package com.inhatc.auction.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.inhatc.auction.common.constant.AuctionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuctionDetailResponseDTO {
    private Long id;
    private Long userId;
    private String nickname;
    private String categoryName;
    private String title;
    private String description;
    private Long startPrice;
    private Long currentPrice;
    private Long buyNowPrice;
    private Long bidCount;
    private Long watchCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auctionStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auctionEndTime;

    private Long auctionLeftTime;
    private String highestBidderNickname;
    private AuctionStatus status;
    private List<ImageResponseDTO> images;
    private List<BidResponseDTO> bids;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}