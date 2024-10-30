package com.inhatc.auction.dto;

import com.inhatc.auction.common.constant.TransactionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponseDTO {
    private Long userId;
    private String nickname;
    private TransactionStatus status;
}
