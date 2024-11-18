package com.inhatc.auction.domain.auction.websocket;

import com.inhatc.auction.global.constant.TransactionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketResponseDTO {
    private String type;
    private int status;
    private Object data;

    @Getter
    @Builder
    public static class Message {
        private String message;
    }

    @Getter
    @Builder
    public static class BidData {
        private Long id;
        private Long userId;
        private String nickname;
        private Long bidAmount;
        private String createdAt;
        private Long auctionLeftTime;
    }

    @Getter
    @Builder
    public static class BidResponse {
        private String message;
        private BidData bidData;
    }

    @Getter
    @Builder
    public static class BuyNowData {
        private Long userId;
        private String nickname;
        private String status;
    }

    @Getter
    @Builder
    public static class BuyNowResponse {
        private String message;
        private BuyNowData buyNowData;
    }

    @Getter
    @Builder
    public static class TransactionData {
        private Long userId;
        private String nickname;
        private TransactionStatus status;
        private Long successfulPrice;
    }

    @Getter
    @Builder
    public static class TransactionResponse {
        private String message;
        private TransactionData transactionData;
    }
}
