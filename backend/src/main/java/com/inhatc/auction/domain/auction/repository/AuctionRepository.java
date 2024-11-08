package com.inhatc.auction.domain.auction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import com.inhatc.auction.domain.auction.entity.Auction;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    // 현재 시간보다 경매 종료 시간 + minutes분 이후인 경매 조회
    @Query("SELECT a FROM Auction a WHERE DATEADD(MINUTE, :minutes, a.auctionEndTime) > NOW() ORDER BY a.auctionStartTime DESC")
    List<Auction> findAllByAuctionEndTimeAfter(@Param("minutes") Integer minutes);

    // 특정 경매 조회
    @NonNull
    Optional<Auction> findById(@NonNull Long auctionId);

    // 모든 경매 조회
    @NonNull
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
}
