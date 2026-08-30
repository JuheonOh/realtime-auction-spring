package com.inhatc.auction.global.outbox.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.auction.domain.bid.websocket.WebSocketHandler;
import com.inhatc.auction.domain.notification.dto.response.AuctionInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.domain.notification.service.SseNotificationService;
import com.inhatc.auction.global.outbox.entity.OutboxEvent;
import com.inhatc.auction.global.outbox.repository.OutboxEventRepository;

class OutboxDispatcherTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private OutboxEventRepository outboxEventRepository;
    private SseNotificationService sseNotificationService;
    private WebSocketHandler webSocketHandler;
    private OutboxDispatcher outboxDispatcher;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        sseNotificationService = mock(SseNotificationService.class);
        webSocketHandler = mock(WebSocketHandler.class);
        outboxDispatcher = new OutboxDispatcher(outboxEventRepository, sseNotificationService, webSocketHandler,
                objectMapper);
    }

    @Test
    void dispatchesSsePayloadToItsRecipientAndMarksItDeliveredWithoutDeletingIt() throws Exception {
        NotificationResponseDTO payload = notificationPayload();
        OutboxEvent event = OutboxEvent.sse(31L, objectMapper.writeValueAsString(payload));
        dueEventsAre(event);

        outboxDispatcher.dispatchDueEvents();

        ArgumentCaptor<NotificationResponseDTO> payloadCaptor = ArgumentCaptor.forClass(NotificationResponseDTO.class);
        verify(sseNotificationService).sendNotification(eq(31L), payloadCaptor.capture());
        NotificationResponseDTO dispatched = payloadCaptor.getValue();
        assertEquals(payload.getId(), dispatched.getId());
        assertEquals(payload.getType(), dispatched.getType());
        assertEquals(payload.getAuctionInfo().getId(), dispatched.getAuctionInfo().getId());
        assertEquals(payload.getAuctionInfo().getAuctionEndTime(), dispatched.getAuctionInfo().getAuctionEndTime());
        assertNotNull(event.getDeliveredAt());
        assertEquals(0, event.getAttempts());
        assertNull(event.getLastError());
        verify(outboxEventRepository, never()).deleteByDeliveredAtBefore(any(LocalDateTime.class));
    }

    @Test
    void dispatchesWebSocketPayloadWithTheDurableEventIdAndMarksItDelivered() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"bidAmount\":55000,\"nickname\":\"buyer\"}");
        OutboxEvent event = OutboxEvent.webSocket(82L, "bid", 201, objectMapper.writeValueAsString(payload));
        dueEventsAre(event);

        outboxDispatcher.dispatchDueEvents();

        ArgumentCaptor<JsonNode> payloadCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(webSocketHandler).broadcastOutbox(eq(event.getEventId()), eq(82L), eq("bid"), eq(201),
                payloadCaptor.capture());
        assertEquals(payload, payloadCaptor.getValue());
        assertNotNull(event.getDeliveredAt());
        assertEquals(0, event.getAttempts());
        verify(outboxEventRepository, never()).deleteByDeliveredAtBefore(any(LocalDateTime.class));
    }

    @Test
    void failedDeliveryRecordsAttemptAndSchedulesFutureRetryWithoutDeletingTheEvent() throws Exception {
        OutboxEvent event = OutboxEvent.sse(44L, objectMapper.writeValueAsString(notificationPayload()));
        dueEventsAre(event);
        doThrow(new IllegalStateException("subscriber unavailable"))
                .when(sseNotificationService).sendNotification(anyLong(), any(NotificationResponseDTO.class));
        LocalDateTime beforeDispatch = LocalDateTime.now();

        outboxDispatcher.dispatchDueEvents();

        assertEquals(1, event.getAttempts());
        assertNull(event.getDeliveredAt());
        assertEquals("IllegalStateException: subscriber unavailable", event.getLastError());
        assertTrue(event.getNextAttemptAt().isAfter(beforeDispatch));
        assertFalse(event.getNextAttemptAt().isAfter(LocalDateTime.now().plusSeconds(3)));
        verify(sseNotificationService).sendNotification(eq(44L), any(NotificationResponseDTO.class));
        verify(webSocketHandler, never()).broadcastOutbox(anyString(), anyLong(), anyString(), any(Integer.class),
                any(JsonNode.class));
        verify(outboxEventRepository, never()).deleteByDeliveredAtBefore(any(LocalDateTime.class));
    }

    private void dueEventsAre(OutboxEvent event) {
        when(outboxEventRepository
                .findFirst100ByDeliveredAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(event));
    }

    private NotificationResponseDTO notificationPayload() {
        return NotificationResponseDTO.builder()
                .id(18L)
                .type(NotificationType.BID)
                .isRead(false)
                .time("just now")
                .auctionInfo(AuctionInfoDTO.builder()
                        .id(82L)
                        .title("Vintage camera")
                        .currentPrice(55000L)
                        .auctionEndTime(LocalDateTime.of(2026, 8, 30, 12, 0))
                        .build())
                .build();
    }
}
