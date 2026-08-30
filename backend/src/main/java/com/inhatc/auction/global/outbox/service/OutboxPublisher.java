package com.inhatc.auction.global.outbox.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.global.outbox.entity.OutboxEvent;
import com.inhatc.auction.global.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public String publishSse(Long recipientUserId, NotificationResponseDTO payload) {
        if (recipientUserId == null || payload == null) {
            throw new IllegalArgumentException("SSE recipient and payload are required");
        }

        return outboxEventRepository.save(OutboxEvent.sse(recipientUserId, serialize(payload))).getEventId();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String publishWebSocket(Long auctionId, String eventType, int status, JsonNode data) {
        if (auctionId == null || eventType == null || eventType.isBlank() || data == null) {
            throw new IllegalArgumentException("WebSocket auction, type, and data are required");
        }

        return outboxEventRepository.save(OutboxEvent.webSocket(auctionId, eventType, status, serialize(data))).getEventId();
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Outbox payload could not be serialized", e);
        }
    }
}
