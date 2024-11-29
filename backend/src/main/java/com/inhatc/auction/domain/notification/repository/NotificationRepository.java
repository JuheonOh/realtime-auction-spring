package com.inhatc.auction.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 유저 아이디와 삭제되지 않은 알림 조회
    List<Notification> findByUserIdAndIsDeletedFalse(Long userId);

    // 유저 아이디와 알림 타입과 경매 아이디로 알림 조회
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.auctionId = :auctionId AND n.isDeleted = FALSE")
    Optional<Notification> findDuplicatedNotification(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("auctionId") Long auctionId);
}
