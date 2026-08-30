package com.inhatc.auction.global.config;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.inhatc.auction.domain.bid.websocket.WebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketHandler webSocketHandler;

    @Value("${app.security.frontend-origin:${FRONTEND_ORIGIN:http://localhost:3000}}")
    private String frontendOrigin;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(Objects.requireNonNull(webSocketHandler), "/ws/auctions/{auctionId}")
                .setAllowedOrigins(frontendOrigin);
    }
}
