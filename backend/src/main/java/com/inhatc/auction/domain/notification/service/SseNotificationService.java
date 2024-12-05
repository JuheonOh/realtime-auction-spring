package com.inhatc.auction.domain.notification.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.entity.AuctionStatus;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.favorite.entity.Favorite;
import com.inhatc.auction.domain.favorite.repository.FavoriteRepository;
import com.inhatc.auction.domain.notification.dto.response.AuctionInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.entity.Notification;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.domain.notification.repository.NotificationRepository;
import com.inhatc.auction.global.jwt.JwtTokenProvider;
import com.inhatc.auction.global.utils.TimeUtils;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class SseNotificationService {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuctionRepository auctionRepository;
    private final FavoriteRepository favoriteRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>(); // 사용자 ID에 따른 SseEmitter 리스트
    private static final Long DEFAULT_TIMEOUT = 1000L * 60 * 10; // 10분

    // SSE 연결 생성
    public SseEmitter subscribe(HttpServletRequest request, Long userId) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        if (accessToken == null) {
            log.error("액세스 토큰이 없습니다.");
            return null;
        }

        Long tokenUserId = jwtTokenProvider.getUserIdFromToken(accessToken);
        if (!jwtTokenProvider.validateToken(accessToken) || userId != tokenUserId) {
            log.error("토큰 검증 실패 또는 사용자 ID 불일치");
            return null;
        }

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 사용자 ID에 따른 SseEmitter 리스트 생성
        List<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>());

        try {
            // 사용자 ID에 따른 SseEmitter 리스트에 연결 완료 메시지 전송
            List<NotificationResponseDTO> notifications = notificationService.getNotifications(request);

            if (!notifications.isEmpty()) {
                emitter.send(SseEmitter.event().name("connect").data(notifications));
            } else {
                emitter.send(SseEmitter.event().name("connect").data(new ArrayList<>()));
            }

            userEmitters.add(emitter);

            // SseEmitter 콜백 등록
            registerEmitterCallbacks(userId, emitter);

            return emitter;
        } catch (IOException e) {
            log.error("SseNotificationService - IOException 발생 : {}", e.getMessage());
            emitter.complete();
        }

        return null;
    }

    // 알림 전송
    public void sendNotification(Long userId, NotificationResponseDTO notificationResponseDTO) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : userEmitters) {
            if (emitter == null) {
                deadEmitters.add(emitter);
                continue;
            }

            try {
                emitter.send(SseEmitter.event().name("notification").data(notificationResponseDTO));
            } catch (IOException e) {
                log.error("알림 전송 실패 (사용자 ID: {})", userId, e);
                deadEmitters.add(emitter);

                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // 이미 완료된 emitter는 무시
                }
            }
        }

        // 죽은 연결 제거
        if (!deadEmitters.isEmpty()) {
            userEmitters.removeAll(deadEmitters);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    // 즐겨찾기 경매 종료 1시간 전 알림 전송
    @Transactional
    @Scheduled(fixedRate = 60000) // 매 분마다 실행
    public void sendEndingSoonNotifications() {
        LocalDateTime oneHourLater = LocalDateTime.now().plusHours(1);

        // 1시간 뒤에 종료되는 경매 조회
        List<Auction> endingSoonAuctions = auctionRepository.findByAuctionEndTimeBeforeAndStatus(oneHourLater,
                AuctionStatus.ACTIVE);

        for (Auction auction : endingSoonAuctions) {
            List<Favorite> favorites = favoriteRepository.findByAuction(auction);

            for (Favorite favorite : favorites) {
                // 이미 알림을 보냈는지 확인
                if (notificationRepository.existsByUserAndAuctionIdAndType(favorite.getUser(), auction.getId(),
                        NotificationType.REMINDER)) {
                    continue;
                }

                Notification notification = Notification.builder()
                        .user(favorite.getUser())
                        .auctionId(auction.getId())
                        .type(NotificationType.REMINDER)
                        .build();

                notificationRepository.save(notification);

                NotificationResponseDTO notificationDTO = NotificationResponseDTO.builder()
                        .id(notification.getId())
                        .type(NotificationType.REMINDER)
                        .isRead(notification.getIsRead())
                        .time(TimeUtils.getRelativeTimeString(notification.getCreatedAt()))
                        .auctionInfo(AuctionInfoDTO.builder()
                                .id(auction.getId())
                                .title(auction.getTitle())
                                .currentPrice(auction.getCurrentPrice())
                                .filePath(auction.getImages().get(0).getFilePath())
                                .fileName(auction.getImages().get(0).getFileName())
                                .auctionEndTime(auction.getAuctionEndTime())
                                .build())
                        .build();

                this.sendNotification(favorite.getUser().getId(), notificationDTO);
            }
        }
    }

    // SSE 연결 유지
    @Scheduled(fixedRate = 30000) // 30초마다 실행
    public void sendPing() {
        emitters.forEach((userId, userEmitters) -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            userEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("ping"));
                } catch (IOException e) {
                    log.error("Ping 전송 실패 : {}", e.getMessage());
                    deadEmitters.add(emitter);

                    try {
                        emitter.complete();
                    } catch (Exception ignored) {
                        // 이미 완료된 emitter는 무시
                    }
                }
            });

            // 죽은 연결 제거
            if (!deadEmitters.isEmpty()) {
                userEmitters.removeAll(deadEmitters);
                if (userEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        });
    }

    // SseEmitter 콜백 등록
    private void registerEmitterCallbacks(Long userId, SseEmitter emitter) {
        // 연결 종료 시
        emitter.onCompletion(() -> {
            log.info("SSE 연결 종료 - userId: {}", userId);
            removeEmitterQuietly(userId, emitter);
        });

        // 타임아웃 시
        emitter.onTimeout(() -> {
            log.info("SseEmitter 타임아웃 (사용자 ID: {})", userId);
            emitter.complete();
            removeEmitterQuietly(userId, emitter);
        });

        // 에러 발생 시
        emitter.onError((e) -> {
            log.error("SseEmitter 에러 발생 (사용자 ID: {})", userId, e);
            removeEmitterQuietly(userId, emitter);
        });
    }

    // 조용히 emitter 제거 (예외 발생하지 않음)
    private synchronized void removeEmitterQuietly(Long userId, SseEmitter emitter) {
        if (userId == null || emitter == null) {
            log.warn("사용자 ID 또는 emitter가 null입니다");
            return;
        }

        try {
            List<SseEmitter> userEmitters = emitters.get(userId);
            if (userEmitters != null) {
                userEmitters.remove(emitter);
                if (userEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        } catch (Exception e) {
            log.error("SSE 연결 제거 중 오류 발생 (사용자 ID: {}): {}", userId, e.getMessage());
        }
    }

    // 서버 종료 시 모든 SseEmitter 종료
    @PreDestroy
    public void destroy() {
        emitters.forEach((userId, emitterList) -> emitterList.forEach(SseEmitter::complete));
        emitters.clear();
    }
}
