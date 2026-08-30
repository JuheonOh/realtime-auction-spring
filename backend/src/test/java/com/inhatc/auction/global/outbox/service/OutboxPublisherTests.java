package com.inhatc.auction.global.outbox.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.auction.domain.notification.dto.response.AuctionInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.MyBidInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.dto.response.PreviousBidInfoDTO;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.global.outbox.entity.OutboxEvent;
import com.inhatc.auction.global.outbox.repository.OutboxEventRepository;

import static org.mockito.Mockito.mock;

class OutboxPublisherTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private OutboxEventRepository outboxEventRepository;
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        outboxPublisher = new OutboxPublisher(outboxEventRepository, objectMapper);
        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void publishSsePersistsNestedNotificationPayloadWithItsGeneratedEventId() throws Exception {
        NotificationResponseDTO notification = notificationPayload();

        String eventId = outboxPublisher.publishSse(71L, notification);

        OutboxEvent event = savedEvent();
        NotificationResponseDTO restored = objectMapper.readValue(event.getPayload(), NotificationResponseDTO.class);
        assertNotNull(eventId);
        assertTrue(!eventId.isBlank());
        assertEquals(event.getEventId(), eventId);
        assertEquals(OutboxEvent.CHANNEL_SSE, event.getChannel());
        assertEquals(71L, event.getRecipientUserId());
        assertNull(event.getAuctionId());
        assertNull(event.getEventType());
        assertNull(event.getStatus());
        assertEquals(notification.getId(), restored.getId());
        assertEquals(notification.getType(), restored.getType());
        assertEquals(notification.getIsRead(), restored.getIsRead());
        assertEquals(notification.getTime(), restored.getTime());
        assertEquals(notification.getAuctionInfo().getId(), restored.getAuctionInfo().getId());
        assertEquals(notification.getAuctionInfo().getAuctionEndTime(), restored.getAuctionInfo().getAuctionEndTime());
        assertEquals(notification.getMyBidInfo().getBidAmount(), restored.getMyBidInfo().getBidAmount());
        assertEquals(notification.getPreviousBidInfo().getBidAmount(), restored.getPreviousBidInfo().getBidAmount());
        verifyNoMoreInteractions(outboxEventRepository);
    }

    @Test
    void publishWebSocketPersistsStableEventIdTargetAndJsonPayloadOnly() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"bidAmount\":45000,\"nickname\":\"buyer\"}");

        String eventId = outboxPublisher.publishWebSocket(93L, "bid", 201, payload);

        OutboxEvent event = savedEvent();
        assertNotNull(eventId);
        assertTrue(!eventId.isBlank());
        assertEquals(event.getEventId(), eventId);
        assertEquals(OutboxEvent.CHANNEL_WEBSOCKET, event.getChannel());
        assertNull(event.getRecipientUserId());
        assertEquals(93L, event.getAuctionId());
        assertEquals("bid", event.getEventType());
        assertEquals(201, event.getStatus());
        assertEquals(payload, objectMapper.readTree(event.getPayload()));
        verifyNoMoreInteractions(outboxEventRepository);
    }

    private OutboxEvent savedEvent() {
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private NotificationResponseDTO notificationPayload() {
        return NotificationResponseDTO.builder()
                .id(15L)
                .type(NotificationType.OUTBID)
                .isRead(false)
                .time("just now")
                .auctionInfo(AuctionInfoDTO.builder()
                        .id(22L)
                        .title("Vintage camera")
                        .currentPrice(45000L)
                        .successfulPrice(0L)
                        .filePath("/images/camera.jpg")
                        .fileName("camera.jpg")
                        .auctionEndTime(LocalDateTime.of(2026, 8, 30, 12, 0))
                        .build())
                .myBidInfo(MyBidInfoDTO.builder().bidAmount(41000L).build())
                .previousBidInfo(PreviousBidInfoDTO.builder().bidAmount(40000L).build())
                .build();
    }
}
