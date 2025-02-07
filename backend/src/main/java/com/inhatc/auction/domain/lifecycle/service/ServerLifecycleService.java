package com.inhatc.auction.domain.lifecycle.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.entity.AuctionStatus;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.lifecycle.entity.ServerLifecycle;
import com.inhatc.auction.domain.lifecycle.repository.ServerLifecycleRepository;
import com.inhatc.auction.domain.lifecycle.util.FileUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerLifecycleService {
    private final ServerLifecycleRepository serverLifecycleRepository;
    private final AuctionRepository auctionRepository;
    private final FileUtils fileUtils;
    // private final BidMigrationService bidMigrationService;

    @Transactional
    public void handleStartup() {
        LocalDateTime startupTime = LocalDateTime.now();
        LocalDateTime lastShutdownTime = fileUtils.readLastShutdownTime();

        // 마지막 종료 기록을 찾기
        ServerLifecycle lifecycle = serverLifecycleRepository
                .findLastShutdownTime(lastShutdownTime)
                .orElse(null);

        if (lifecycle != null) {
            // 서버 시작 시간 기록
            lifecycle.recordStartup(startupTime);

            // 경매 종료 시간 보상 처리
            compensateAuctionEndTimes(lastShutdownTime, startupTime);

            // 보상 처리 완료
            lifecycle.successCompensation();

            // 보상 처리 완료 메시지 로그
            log.info("경매 종료 시간 보상 처리 완료");
        }

        // 입찰 데이터 마이그레이션
        // bidMigrationService.migrateAllBidsToRedis();
    }

    @Transactional
    public void handleShutdown() {
        LocalDateTime shutdownTime = LocalDateTime.now(); // 현재 시간 기록
        fileUtils.writeShutdownTime(shutdownTime); // 파일 시스템에 종료 시간 기록 (DB가 이미 종료되었을 수 있으므로)

        try {
            // 새로운 종료 기록 생성
            serverLifecycleRepository.save(ServerLifecycle.builder().shutdownTime(shutdownTime).build());
        } catch (Exception e) {
            log.error("데이터베이스에 서버 종료 시간 기록 실패", e);
        }

        // 입찰 데이터 마이그레이션
        // bidMigrationService.migrateAllBidsToMariaDB();
    }

    @Transactional
    private void compensateAuctionEndTimes(LocalDateTime serverStopTime, LocalDateTime serverStartTime) {
        // 다운타임 시간 계산
        Duration downtime = Duration.between(serverStopTime, serverStartTime);
        log.info("서버 다운타임: {}", serverStopTime);
        log.info("서버 재시작 시간: {}", serverStartTime);
        log.info("서버 다운타임 시간: {}", downtime);

        if (downtime.toMinutes() < 1) {
            return;
        }

        // 보상이 필요한 경매 조회
        List<Auction> auctions = auctionRepository.findAuctionsForDowntimeCompensation(
                AuctionStatus.ACTIVE,
                serverStopTime);

        // 보상 처리
        for (Auction auction : auctions) {
            // 경매들 다운타임만큼 종료시간 보상
            // 경매 종료 시간 연장 (다운타임 + 5분 연장)
            auction.extendEndTime(downtime.plusMinutes(10));
        }

        auctionRepository.saveAll(auctions);
        log.info("경매 종료 시간 보상 처리 완료");
    }
}
