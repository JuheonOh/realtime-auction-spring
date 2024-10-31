package com.inhatc.auction.domain.transaction.dto.response;

import com.inhatc.auction.global.constant.TransactionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponseDTO {
    private Long userId;
    private String nickname;
    private TransactionStatus status;
}
