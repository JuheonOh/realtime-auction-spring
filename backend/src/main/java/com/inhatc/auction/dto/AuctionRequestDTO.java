package com.inhatc.auction.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class AuctionRequestDTO {
    private Long userId;
    private String title;
    private String description;
    private Long categoryId;
    private Long startPrice;
    private Long buyNowPrice;
    private Integer auctionDuration;
    private List<MultipartFile> images;
}