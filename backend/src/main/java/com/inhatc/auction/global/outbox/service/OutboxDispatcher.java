package com.inhatc.auction.global.outbox.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.auction.domain.bid.websocket.WebSocketHandler;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.service.SseNotificationService;
import com.inhatc.auction.global.outbox.entity.OutboxEvent;
import com.inhatc.auction.global.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OutboxDispatcher {

    private static final long MAX_RETRY_DELAY_SECONDS = 300;

    private final OutboxEventRepository outboxEventRepository;
    private final SseNotificationService sseNotificationService;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void dispatchDueEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findFirst100ByDeliveredAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(LocalDateTime.now());

        for (OutboxEvent event : events) {
            try {
                deliver(event);
                event.markDelivered();
            } catch (Exception e) {
                event.recordFailure(describe(e), LocalDateTime.now().plusSeconds(backoffSeconds(event.getAttempts())));
                log.warn("Outbox delivery failed: eventId={}, channel={}, attempt={}", event.getEventId(),
                        event.getChannel(), event.getAttempts(), e);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteDeliveredEvents() {
        long deleted = outboxEventRepository.deleteByDeliveredAtBefore(
                LocalDateTime.now().minus(7, ChronoUnit.DAYS));
        if (deleted > 0) {
            log.info("Deleted {} delivered outbox events", deleted);
        }
    }

    private void deliver(OutboxEvent event) throws Exception {
        if (OutboxEvent.CHANNEL_SSE.equals(event.getChannel())) {
            NotificationResponseDTO payload = objectMapper.readValue(event.getPayload(), NotificationResponseDTO.class);
            sseNotificationService.sendNotification(event.getRecipientUserId(), payload);
            return;
        }

        if (OutboxEvent.CHANNEL_WEBSOCKET.equals(event.getChannel())) {
            JsonNode data = objectMapper.readTree(event.getPayload());
            webSocketHandler.broadcastOutbox(event.getEventId(), event.getAuctionId(), event.getEventType(),
                    event.getStatus(), data);
            return;
        }

        throw new IllegalStateException("Unsupported outbox channel: " + event.getChannel());
    }

    private long backoffSeconds(int attempts) {
        int exponent = Math.min(attempts, 8);
        return Math.min(1L << exponent, MAX_RETRY_DELAY_SECONDS);
    }

    private String describe(Exception exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
