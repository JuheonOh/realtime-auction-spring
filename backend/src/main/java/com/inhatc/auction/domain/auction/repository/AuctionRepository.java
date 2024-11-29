package com.inhatc.auction.domain.auction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.entity.AuctionStatus;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

        // 현재 시간보다 경매 종료 시간 + minutes분 이후인 경매 조회
        @Query("SELECT a FROM Auction a WHERE DATEADD(MINUTE, :minutes, a.auctionEndTime) > NOW() ORDER BY a.auctionStartTime DESC")
        List<Auction> findAllByAuctionEndTimeAfter(@Param("minutes") Integer minutes);

        // 특정 경매 조회
        @NonNull
        @Query("SELECT a FROM Auction a WHERE a.id = :auctionId")
        @Override
        Optional<Auction> findById(@NonNull @Param("auctionId") Long auctionId);

        // 모든 경매 조회
        @NonNull
        @Query("SELECT a FROM Auction a ORDER BY a.auctionStartTime DESC")
        @Override
        List<Auction> findAll();

        // 남은 시간 계산
        @Query("SELECT TIMESTAMPDIFF(SECOND, NOW(), a.auctionEndTime) FROM Auction a WHERE a.id = :auctionId")
        Long calculateAuctionLeftTime(@Param("auctionId") Long auctionId);

        // 경매 종료 시간 조회
        @Query("SELECT a.auctionEndTime FROM Auction a WHERE a.id = :auctionId")
        LocalDateTime findAuctionEndTimeById(@Param("auctionId") Long auctionId);

        // 경매 입찰 수 내림차순 조회 (주목할 만한 경매)
        @Query("SELECT a FROM Auction a WHERE a.auctionEndTime > NOW() ORDER BY (SELECT COUNT(b) FROM Bid b WHERE b.auction.id = a.id) DESC LIMIT 4")
        List<Auction> findAllByOrderByBidCountDesc();

        // 서버 중단 시점 이전에 종료되지 않은 모든 경매 조회
        @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.auctionEndTime > :serverStopTime")
        List<Auction> findAuctionsForDowntimeCompensation(
                        @Param("status") AuctionStatus status,
                        @Param("serverStopTime") LocalDateTime serverStopTime);

        // 종료된 경매 조회
        List<Auction> findByAuctionEndTimeBeforeAndStatus(@Param("auctionEndTime") LocalDateTime auctionEndTime,
                        @Param("status") AuctionStatus status);

        // 종료 시간 사이에 있는 경매 조회
        List<Auction> findByAuctionEndTimeBetweenAndStatus(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime,
                        @Param("status") AuctionStatus status);

        // 종료 시간 이후인 경매 조회
        List<Auction> findByAuctionEndTimeAfterAndStatus(@Param("auctionEndTime") LocalDateTime auctionEndTime,
                        @Param("status") AuctionStatus status);

        @Query("SELECT a FROM Auction a LEFT JOIN FETCH a.images WHERE a.id = :id")
        Optional<Auction> findByIdWithImages(@Param("id") Long id);
}
