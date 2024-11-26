package com.inhatc.auction.domain.auction.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.inhatc.auction.domain.bid.dto.response.BidResponseDTO;
import com.inhatc.auction.domain.image.dto.response.ImageResponseDTO;
import com.inhatc.auction.domain.transaction.dto.response.TransactionResponseDTO;
import com.inhatc.auction.global.constant.AuctionStatus;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
    private Long successfulPrice;
    private Long bidCount;
    private Long favoriteCount;
    private Boolean isFavorite;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auctionStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auctionEndTime;

    private Long auctionLeftTime;
    private AuctionStatus status;
    private List<ImageResponseDTO> images;
    private List<BidResponseDTO> bids;
    private TransactionResponseDTO transaction;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}