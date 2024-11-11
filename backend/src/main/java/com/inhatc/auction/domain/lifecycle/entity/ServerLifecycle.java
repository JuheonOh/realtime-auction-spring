package com.inhatc.auction.domain.lifecycle.entity;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "server_lifecycle")
@Getter
@NoArgsConstructor
public class ServerLifecycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startupTime; // 서버 시작 시간
    private LocalDateTime shutdownTime; // 서버 종료 시간
    private Long downtime; // 서버 다운 시간
    private boolean isCompensated = false; // 보상 여부

    @Builder
    public ServerLifecycle(LocalDateTime shutdownTime) {
        this.shutdownTime = shutdownTime;
        this.isCompensated = false;
    }

    public void recordStartup(LocalDateTime startupTime) {
        this.startupTime = startupTime;
        this.downtime = Duration.between(shutdownTime, startupTime).toSeconds();
    }

    public void successCompensation() {
        this.isCompensated = true;
    }
}
