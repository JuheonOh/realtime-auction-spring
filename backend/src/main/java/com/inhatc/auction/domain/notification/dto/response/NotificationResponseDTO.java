package com.inhatc.auction.domain.notification.dto.response;

import com.inhatc.auction.domain.notification.entity.NotificationType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponseDTO {
    private Long id;
    private NotificationType type;
    private Boolean isRead;
    private String time;
    private AuctionInfoDTO auctionInfo;
    private MyBidInfoDTO myBidInfo;
    private PreviousBidInfoDTO previousBidInfo;
}
