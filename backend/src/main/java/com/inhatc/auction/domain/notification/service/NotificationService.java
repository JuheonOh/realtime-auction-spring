package com.inhatc.auction.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.entity.Notification;
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

    // 사용자의 알림 목록 조회
    public List<NotificationResponseDTO> getNotifications(HttpServletRequest request) {
        String accessToken = jwtTokenProvider.getTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        List<Notification> notifications = notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        return notifications.stream().<NotificationResponseDTO>map(notification -> {
            LocalDateTime createdAt = notification.getCreatedAt();
            String time = TimeUtils.getRelativeTimeString(createdAt);

            return NotificationResponseDTO.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .isRead(notification.getIsRead())
                    .time(time)
                    .build();
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
