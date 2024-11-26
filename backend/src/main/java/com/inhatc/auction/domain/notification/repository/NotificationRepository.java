package com.inhatc.auction.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.notification.entity.Notification;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.domain.user.entity.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 유저 아이디로 알림 조회 (최근 순)
    List<Notification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    // 유저 아이디로 읽지 않은 알림 조회
    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    // 유저 아이디와 알림 아이디로 알림 조회
    Optional<Notification> findByIdAndUserIdAndIsDeletedFalse(Long notificationId, Long userId);

    // 유저 아이디와 알림 타입으로 알림 조회
    boolean existsByUserAndType(User user, NotificationType type);

    // 유저 아이디와 경매 아이디와 알림 타입으로 알림 조회
    boolean existsByUserAndAuctionIdAndType(User user, Long auctionId, NotificationType type);
}
