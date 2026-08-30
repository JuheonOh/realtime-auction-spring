package com.inhatc.auction.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class PreviousBidInfoDTO {
    private Long bidAmount;
}
