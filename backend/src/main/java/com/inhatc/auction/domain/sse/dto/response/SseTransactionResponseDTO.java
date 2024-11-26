package com.inhatc.auction.domain.sse.dto.response;

import com.inhatc.auction.domain.transaction.entity.TransactionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseTransactionResponseDTO {
    private Long userId;
    private String nickname;
    private TransactionStatus status;
}
