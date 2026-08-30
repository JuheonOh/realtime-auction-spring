package com.inhatc.auction.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.entity.Bid;
import com.inhatc.auction.domain.bid.repository.BidRepository;
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
    private final BidRepository bidRepository;

    // 사용자의 알림 목록 조회
    public List<NotificationResponseDTO> getNotifications(@NonNull HttpServletRequest request) {
        Long userId = extractUserIdOrThrow(request);

        log.info("userId: {}", userId);

        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        return notifications.stream().<NotificationResponseDTO>map(notification -> {
            LocalDateTime createdAt = notification.getCreatedAt();
            String time = TimeUtils.getRelativeTimeString(createdAt);

            Long auctionId = notification.getAuctionId();
            if (auctionId == null) {
                return null;
            }
            Optional<Auction> auctionOptional = auctionRepository.findById(auctionId);

            // 경매 정보가 없는 경우
            if (auctionOptional.isEmpty()) {
                return null;
            }

            NotificationType type = notification.getType();
            if (type == NotificationType.BID) {
                // 이전 최고 입찰 정보와 현재 최고 입찰 정보 조회 (이전 최고 입찰자, 현재 입찰자)
                List<Bid> bidList = bidRepository.findByAuctionId(auctionId);
                Boolean isPreviousBidPresent = bidList.size() >= 2; // 이전 최고 입찰 정보가 있는 경우

                // 경매 정보가 있고 입찰 정보가 있는 경우
                if (auctionOptional.isPresent() && !bidList.isEmpty()) {
                    Auction auction = auctionOptional.get();

                    // 이전 최고 입찰 정보가 있는 경우
                    if (isPreviousBidPresent) {
                        Bid currentBid = bidList.get(0);
                        Bid previousBid = bidList.get(1);

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
                        Bid currentBid = bidList.get(0);

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
                List<Bid> allBidsList = bidRepository.findByAuctionId(auctionId);
                Optional<Bid> myBidOptional = bidRepository.findFirstByUserIdAndAuctionIdOrderByBidAmountDesc(userId,
                        auctionId);

                if (auctionOptional.isPresent() && myBidOptional.isPresent() && !allBidsList.isEmpty()) {
                    Auction auction = auctionOptional.get();
                    Bid myBid = myBidOptional.get(); // 내 입찰가

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
            } else if (type == NotificationType.WIN || type == NotificationType.BUY_NOW_WIN) {
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
                                    .successfulPrice(auction.getSuccessfulPrice())
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
            } else if (type == NotificationType.ENDED || type == NotificationType.ENDED_TIME) {
                if (auctionOptional.isPresent()) {
                    Auction auction = auctionOptional.get();
                    Optional<Bid> myBidOptional = bidRepository.findFirstByUserIdAndAuctionIdOrderByBidAmountDesc(
                            userId, auctionId);
                    MyBidInfoDTO myBidInfoDTO = myBidOptional.isEmpty()
                            ? null
                            : MyBidInfoDTO.builder()
                                    .bidAmount(myBidOptional.get().getBidAmount())
                                    .build();

                    NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
                            .id(notification.getId())
                            .type(notification.getType())
                            .isRead(notification.getIsRead())
                            .time(time)
                            .auctionInfo(AuctionInfoDTO.builder()
                                    .id(auction.getId())
                                    .title(auction.getTitle())
                                    .currentPrice(auction.getCurrentPrice())
                                    .successfulPrice(auction.getSuccessfulPrice())
                                    .filePath(auction.getImages().get(0).getFilePath())
                                    .fileName(auction.getImages().get(0).getFileName())
                                    .auctionEndTime(auction.getAuctionEndTime())
                                    .build())
                            .myBidInfo(myBidInfoDTO)
                            .build();

                    return notificationResponseDTO;
                }
            }

            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // 모두 읽음 처리
    public void markAsReadAll(@NonNull HttpServletRequest request) {
        Long userId = extractUserIdOrThrow(request);

        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        notifications.forEach(notification -> {
            notification.markAsRead();
        });

        notificationRepository.saveAll(notifications);
    }

    // 알림 읽음 처리
    public void markAsRead(@NonNull HttpServletRequest request, Long notificationId) {
        Long userId = extractUserIdOrThrow(request);

        notificationRepository.findByIdAndUserIdAndIsDeletedFalse(notificationId, userId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    // 모든 알림 삭제 처리
    public void deleteNotificationAll(@NonNull HttpServletRequest request) {
        Long userId = extractUserIdOrThrow(request);

        List<Notification> notifications = notificationRepository.findByUserIdAndIsDeletedFalse(userId);
        notifications.forEach(notification -> {
            notification.markAsDeleted();
        });

        notificationRepository.saveAll(notifications);
    }

    // 알림 삭제 처리
    public void deleteNotification(@NonNull HttpServletRequest request, Long notificationId) {
        Long userId = extractUserIdOrThrow(request);

        notificationRepository.findByIdAndUserIdAndIsDeletedFalse(notificationId, userId).ifPresent(notification -> {
            notification.markAsRead();
            notification.markAsDeleted();
            notificationRepository.save(notification);
        });
    }

    private Long extractUserIdOrThrow(@NonNull HttpServletRequest request) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        if (accessToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        try {
            if (!jwtTokenProvider.validateToken(accessToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
            }
            return jwtTokenProvider.getUserIdFromToken(accessToken);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", e);
        }
    }

}
