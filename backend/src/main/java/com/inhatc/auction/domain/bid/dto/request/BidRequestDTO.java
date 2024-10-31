package com.inhatc.auction.domain.bid.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BidRequestDTO {
    @Positive(message = "입찰 금액이 필요합니다.")
    private Long bidAmount;
}
