package com.inhatc.auction.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.inhatc.auction.common.validation.BuyNowPriceValidator;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@BuyNowPriceValidator
public class AuctionRequestDTO {
    @NotNull(message = "로그인 정보가 없습니다.")
    private Long userId;

    @NotBlank(message = "제목은 필수 입력 사항입니다.")
    private String title;

    @NotBlank(message = "설명은 필수 입력 사항입니다.")
    private String description;

    @Positive(message = "카테고리는 필수 선택 사항입니다.")
    private Long categoryId;

    @Positive(message = "경매 시작 가격은 필수 입력 사항입니다.")
    @Min(value = 1000, message = "경매 시작 가격은 최소 1,000원 이상이어야 합니다.")
    private Long startPrice;

    @PositiveOrZero(message = "즉시 구매 가격은 0원 이상이어야 합니다.")
    private Long buyNowPrice;

    @Positive(message = "경매 기간은 필수 선택 사항입니다.")
    private Integer auctionDuration;

    @NotEmpty(message = "이미지를 최소 1개 이상 업로드해야 합니다.")
    private List<MultipartFile> images;
}