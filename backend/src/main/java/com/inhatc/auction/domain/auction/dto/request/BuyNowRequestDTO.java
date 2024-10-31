package com.inhatc.auction.domain.auction.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuyNowRequestDTO {
    @NotNull
    private Long userId;
}
