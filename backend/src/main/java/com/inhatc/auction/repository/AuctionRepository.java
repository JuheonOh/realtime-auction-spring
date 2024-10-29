package com.inhatc.auction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import com.inhatc.auction.domain.Auction;

import jakarta.transaction.Transactional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    // 현재시간보다 경매 종료 시간 + minutes분 이후인 경매 조회
    @Query("SELECT a FROM Auction a WHERE DATEADD(MINUTE, :minutes, a.auctionEndTime) > NOW() ORDER BY a.auctionStartTime DESC")
    List<Auction> findAllByAuctionEndTimeAfter(@Param("minutes") Integer minutes);

    @NonNull
    Optional<Auction> findById(@NonNull Long auctionId);

    @NonNull
    List<Auction> findAll();

    @Transactional
    @Modifying
    @Query("UPDATE Auction a SET a.currentPrice = :currentPrice WHERE a.id = :auctionId")
    void updateCurrentPrice(@Param("auctionId") Long auctionId, @Param("currentPrice") Long currentPrice);

    // 남은 시간 계산
    @Query("SELECT TIMESTAMPDIFF(SECOND, NOW(), a.auctionEndTime) FROM Auction a WHERE a.id = :auctionId")
    Long calculateAuctionLeftTime(@Param("auctionId") Long auctionId);

    // 경매 종료 시간 조회
    @Query("SELECT a.auctionEndTime FROM Auction a WHERE a.id = :auctionId")
    LocalDateTime findAuctionEndTimeById(@Param("auctionId") Long auctionId);
}
