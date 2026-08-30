package com.inhatc.auction.global.outbox.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.inhatc.auction.global.outbox.entity.OutboxEvent;

import jakarta.persistence.LockModeType;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findFirst100ByDeliveredAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            LocalDateTime now);

    long deleteByDeliveredAtBefore(LocalDateTime cutoff);
}
