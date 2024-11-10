package com.inhatc.auction.domain.lifecycle.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inhatc.auction.domain.lifecycle.entity.ServerLifecycle;

public interface ServerLifecycleRepository extends JpaRepository<ServerLifecycle, Long> {
    // 마지막 종료 기록 조회
    @Query("SELECT s FROM ServerLifecycle s WHERE s.shutdownTime = :lastShutdownTime AND s.startupTime IS NULL ORDER BY s.id DESC LIMIT 1")
    Optional<ServerLifecycle> findLastShutdownTime(@Param("lastShutdownTime") LocalDateTime lastShutdownTime);

}
