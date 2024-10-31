package com.inhatc.auction.domain.sse.dto.response;

import com.inhatc.auction.global.constant.TransactionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseTransactionResponseDTO {
    private Long userId;
    private String nickname;
    private TransactionStatus status;
}
