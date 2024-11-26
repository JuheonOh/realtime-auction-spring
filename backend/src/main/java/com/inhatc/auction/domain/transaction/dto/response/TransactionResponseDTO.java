package com.inhatc.auction.domain.transaction.dto.response;

import com.inhatc.auction.domain.transaction.entity.TransactionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponseDTO {
    private Long userId;
    private String nickname;
    private TransactionStatus status;
    private Long finalPrice;
}
