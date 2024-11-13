package com.inhatc.auction.domain.lifecycle.handler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.inhatc.auction.domain.lifecycle.service.ServerLifecycleService;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class ServerLifecycleHandler {
    private final ServerLifecycleService serverLifecycleService;

    // 서버 시작 시 호출
    // @PostConstruct //
    @EventListener(ApplicationReadyEvent.class) // AOP 등 Proxy 클래스가 생성된 후 초기화 동작이 필요한 경우
                                                // (AOP 적용 클래스, @Transactional 어노테이션 적용 등)
    public void onStartup() {
        log.info("서버가 시작되었습니다.");
        serverLifecycleService.handleStartup();
    }

    // 서버 종료 시 호출
    @PreDestroy
    public void onShutdown() {
        log.info("서버가 종료되었습니다.");
        serverLifecycleService.handleShutdown();
    }
}