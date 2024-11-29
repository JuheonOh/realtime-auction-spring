package com.inhatc.auction.domain.notification.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.inhatc.auction.domain.notification.dto.request.NotificationRequestDTO;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.service.NotificationService;
import com.inhatc.auction.domain.notification.service.SseNotificationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Log4j2
public class NotificationController {

    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    @GetMapping("/users/notifications")
    public List<NotificationResponseDTO> getNotifications(HttpServletRequest request) {
        return notificationService.getNotifications(request);
    }

    @GetMapping(value = "/users/{userId}/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getNotificationStream(HttpServletRequest request, @PathVariable("userId") Long userId) {
        return sseNotificationService.subscribe(request, userId);
    }

    @PatchMapping("/users/notifications/all")
    public ResponseEntity<?> patchNotificationAll(HttpServletRequest request) {
        notificationService.markAsReadAll(request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/notifications")
    public ResponseEntity<?> patchNotification(HttpServletRequest request,
            @RequestBody NotificationRequestDTO notificationRequestDTO) {
        notificationService.markAsRead(request, notificationRequestDTO.getNotificationId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/notifications/all")
    public ResponseEntity<?> deleteNotificationAll(HttpServletRequest request) {
        notificationService.deleteNotificationAll(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/notifications")
    public ResponseEntity<?> deleteNotification(HttpServletRequest request,
            @RequestBody NotificationRequestDTO notificationRequestDTO) {
        notificationService.deleteNotification(request, notificationRequestDTO.getNotificationId());
        return ResponseEntity.ok().build();
    }
}
