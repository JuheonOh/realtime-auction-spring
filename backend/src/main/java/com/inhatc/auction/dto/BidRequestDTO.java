package com.inhatc.auction.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BidRequestDTO {
    @Positive(message = "입찰 금액이 필요합니다.")
    private Long bidAmount;
}
