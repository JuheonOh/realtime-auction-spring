package com.inhatc.auction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuyNowRequestDTO {
    @NotNull
    private Long userId;
}
