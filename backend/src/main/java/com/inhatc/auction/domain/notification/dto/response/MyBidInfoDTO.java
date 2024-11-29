package com.inhatc.auction.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyBidInfoDTO {
    private Long bidAmount;
}
