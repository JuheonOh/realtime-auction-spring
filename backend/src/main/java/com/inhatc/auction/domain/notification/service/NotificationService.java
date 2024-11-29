package com.inhatc.auction.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.entity.RedisBid;
import com.inhatc.auction.domain.bid.repository.RedisBidRepository;
import com.inhatc.auction.domain.notification.dto.response.AuctionInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.MyBidInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.dto.response.PreviousBidInfoDTO;
import com.inhatc.auction.domain.notification.entity.Notification;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.domain.notification.repository.NotificationRepository;
import com.inhatc.auction.global.jwt.JwtTokenProvider;
import com.inhatc.auction.global.utils.TimeUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final NotificationRepository notificationRepository;
    private final AuctionRepository auctionRepository;
    private final RedisBidRepository redisBidRepository;

    // 사용자의 알림 목록 조회
    public List<NotificationResponseDTO> getNotifications(HttpServletRequest request) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        log.info("userId: {}", userId);

        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        return notifications.stream().<NotificationResponseDTO>map(notification -> {
            LocalDateTime createdAt = notification.getCreatedAt();
            String time = TimeUtils.getRelativeTimeString(createdAt);

            Long auctionId = notification.getAuctionId();
            Optional<Auction> auctionOptional = auctionRepository.findById(auctionId);

            // 경매 정보가 없는 경우
            if (auctionOptional.isEmpty()) {
                return null;
            }

            NotificationType type = notification.getType();
            if (type == NotificationType.BID) {
                // 이전 최고 입찰 정보와 현재 최고 입찰 정보 조회 (이전 최고 입찰자, 현재 입찰자)
                List<RedisBid> redisBidList = redisBidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId);
                Boolean isPreviousBidPresent = redisBidList.size() >= 2; // 이전 최고 입찰 정보가 있는 경우

                // 경매 정보가 있고 입찰 정보가 있는 경우
                if (auctionOptional.isPresent() && !redisBidList.isEmpty()) {
                    Auction auction = auctionOptional.get();

                    // 이전 최고 입찰 정보가 있는 경우
                    if (isPreviousBidPresent) {
                        RedisBid currentBid = redisBidList.get(0);
                        RedisBid previousBid = redisBidList.get(1);

                        NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
                                .id(notification.getId())
                                .type(notification.getType())
                                .isRead(notification.getIsRead())
                                .time(time)
                                .auctionInfo(AuctionInfoDTO.builder()
                                        .id(auction.getId())
                                        .title(auction.getTitle())
                                        .currentPrice(auction.getCurrentPrice())
                                        .filePath(auction.getImages().get(0).getFilePath())
                                        .fileName(auction.getImages().get(0).getFileName())
                                        .auctionEndTime(auction.getAuctionEndTime())
                                        .build())
                                .myBidInfo(MyBidInfoDTO.builder()
                                        .bidAmount(currentBid.getBidAmount())
                                        .build())
                                .previousBidInfo(PreviousBidInfoDTO.builder()
                                        .bidAmount(previousBid.getBidAmount())
                                        .build())
                                .build();

                        return notificationResponseDTO;
                    } else {
                        // 이전 최고 입찰 정보가 없는 경우
                        RedisBid currentBid = redisBidList.get(0);

                        NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
                                .id(notification.getId())
                                .type(notification.getType())
                                .isRead(notification.getIsRead())
                                .time(time)
                                .auctionInfo(AuctionInfoDTO.builder()
                                        .id(auction.getId())
                                        .title(auction.getTitle())
                                        .currentPrice(auction.getCurrentPrice())
                                        .filePath(auction.getImages().get(0).getFilePath())
                                        .fileName(auction.getImages().get(0).getFileName())
                                        .auctionEndTime(auction.getAuctionEndTime())
                                        .build())
                                .myBidInfo(MyBidInfoDTO.builder()
                                        .bidAmount(currentBid.getBidAmount())
                                        .build())
                                .build();

                        return notificationResponseDTO;
                    }
                }

            } else if (type == NotificationType.OUTBID) {
                List<RedisBid> allBidsList = redisBidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId);
                List<RedisBid> myBidsList = redisBidRepository.findByAuctionIdAndUserIdOrderByBidAmountDesc(auctionId,
                        userId);

                if (auctionOptional.isPresent() && !myBidsList.isEmpty() && !allBidsList.isEmpty()) {
                    Auction auction = auctionOptional.get();
                    RedisBid myBid = myBidsList.get(0); // 내 입찰가

                    NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
                            .id(notification.getId())
                            .type(notification.getType())
                            .isRead(notification.getIsRead())
                            .time(time)
                            .auctionInfo(AuctionInfoDTO.builder()
                                    .id(auction.getId())
                                    .title(auction.getTitle())
                                    .currentPrice(auction.getCurrentPrice())
                                    .filePath(auction.getImages().get(0).getFilePath())
                                    .fileName(auction.getImages().get(0).getFileName())
                                    .auctionEndTime(auction.getAuctionEndTime())
                                    .build())
                            .myBidInfo(MyBidInfoDTO.builder()
                                    .bidAmount(myBid.getBidAmount())
                                    .build())
                            .build();

                    return notificationResponseDTO;
                }
            } else if (type == NotificationType.WIN) {
                if (auctionOptional.isPresent()) {
                    Auction auction = auctionOptional.get();

                    NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
                            .id(notification.getId())
                            .type(notification.getType())
                            .isRead(notification.getIsRead())
                            .time(time)
                            .auctionInfo(AuctionInfoDTO.builder()
                                    .id(auction.getId())
                                    .title(auction.getTitle())
                                    .successfulPrice(auction.getCurrentPrice())
                                    .filePath(auction.getImages().get(0).getFilePath())
                                    .fileName(auction.getImages().get(0).getFileName())
                                    .auctionEndTime(auction.getAuctionEndTime())
                                    .build())
                            .build();

                    return notificationResponseDTO;
                }
            } else if (type == NotificationType.REMINDER) {
                if (auctionOptional.isPresent()) {
                    Auction auction = auctionOptional.get();

                    NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
                            .id(notification.getId())
                            .type(notification.getType())
                            .isRead(notification.getIsRead())
                            .time(time)
                            .auctionInfo(AuctionInfoDTO.builder()
                                    .id(auction.getId())
                                    .title(auction.getTitle())
                                    .currentPrice(auction.getCurrentPrice())
                                    .filePath(auction.getImages().get(0).getFilePath())
                                    .fileName(auction.getImages().get(0).getFileName())
                                    .auctionEndTime(auction.getAuctionEndTime())
                                    .build())
                            .build();

                    return notificationResponseDTO;
                }
            }

            return null;
        }).collect(Collectors.toList());
    }

    // 모두 읽음 처리
    public void markAsReadAll(HttpServletRequest request) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        notifications.forEach(notification -> {
            notification.markAsRead();
        });

        notificationRepository.saveAll(notifications);
    }

    // 알림 읽음 처리
    public void markAsRead(HttpServletRequest request, Long notificationId) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        notificationRepository.findByIdAndUserIdAndIsDeletedFalse(notificationId, userId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    // 모든 알림 삭제 처리
    public void deleteNotificationAll(HttpServletRequest request) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        List<Notification> notifications = notificationRepository.findByUserIdAndIsDeletedFalse(userId);
        notifications.forEach(notification -> {
            notification.markAsDeleted();
        });

        notificationRepository.saveAll(notifications);
    }

    // 알림 삭제 처리
    public void deleteNotification(HttpServletRequest request, Long notificationId) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        notificationRepository.findByIdAndUserIdAndIsDeletedFalse(notificationId, userId).ifPresent(notification -> {
            notification.markAsRead();
            notification.markAsDeleted();
            notificationRepository.save(notification);
        });
    }

}
