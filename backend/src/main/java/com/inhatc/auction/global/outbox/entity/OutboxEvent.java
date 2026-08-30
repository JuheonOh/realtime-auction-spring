package com.inhatc.auction.global.outbox.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_event_due", columnList = "delivered_at,next_attempt_at"),
        @Index(name = "idx_outbox_event_auction", columnList = "auction_id")
})
@Getter
@NoArgsConstructor
public class OutboxEvent {

    public static final String CHANNEL_SSE = "SSE";
    public static final String CHANNEL_WEBSOCKET = "WEBSOCKET";

    @Id
    @Column(name = "event_id", length = 36, updatable = false)
    private String eventId;

    @Column(nullable = false, length = 16)
    private String channel;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    private Integer status;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private OutboxEvent(String channel, Long recipientUserId, Long auctionId, String eventType, Integer status,
            String payload) {
        this.eventId = UUID.randomUUID().toString();
        this.channel = channel;
        this.recipientUserId = recipientUserId;
        this.auctionId = auctionId;
        this.eventType = eventType;
        this.status = status;
        this.payload = payload;
        this.nextAttemptAt = LocalDateTime.now();
    }

    public static OutboxEvent sse(Long recipientUserId, String payload) {
        return new OutboxEvent(CHANNEL_SSE, recipientUserId, null, null, null, payload);
    }

    public static OutboxEvent webSocket(Long auctionId, String eventType, int status, String payload) {
        return new OutboxEvent(CHANNEL_WEBSOCKET, null, auctionId, eventType, status, payload);
    }

    public void markDelivered() {
        this.deliveredAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void recordFailure(String error, LocalDateTime retryAt) {
        this.attempts++;
        this.lastError = error == null ? "Unknown delivery failure" : error.substring(0, Math.min(error.length(), 1000));
        this.nextAttemptAt = retryAt;
    }

    @PrePersist
    private void setCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void setUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
