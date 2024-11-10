package com.inhatc.auction.domain.lifecycle.handler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.inhatc.auction.domain.lifecycle.service.ServerLifecycleService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class ServerLifecycleHandler {
    private final ServerLifecycleService serverLifecycleService;

    @EventListener(ApplicationReadyEvent.class) // 서버 시작 시 호출
    public void onStartup() {
        log.info("서버가 시작되었습니다.");
        serverLifecycleService.handleStartup();
    }

    // @PreDestroy // 서버 종료 시 호출
    // public void onShutdown() {
    // log.info("서버가 종료되었습니다.");
    // serverLifecycleService.handleShutdown();
    // }

    @PostConstruct
    public void onShutdown() {
        // Shutdown Hook 등록
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("서버가 종료되었습니다.");
                serverLifecycleService.handleShutdown();
            } catch (Exception e) {
                log.error("서버 종료 시 오류 발생", e);
            }
        }));
    }
}
