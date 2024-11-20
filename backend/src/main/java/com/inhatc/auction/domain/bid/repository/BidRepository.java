package com.inhatc.auction.domain.bid.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inhatc.auction.domain.bid.entity.Bid;

public interface BidRepository extends JpaRepository<Bid, Long> {
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.auction.id = :auctionId")
    Optional<Long> findBidCountByAuctionId(@Param("auctionId") Long auctionId);

    @Query("SELECT b.user.id FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.bidAmount DESC LIMIT 1")
    Optional<Long> findCurrentHighestBidUserId(@Param("auctionId") Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.bidAmount DESC")
    List<Bid> findByAuctionId(@Param("auctionId") Long auctionId);

    // 이미 존재하는 입찰인지 확인
    @Query("SELECT COUNT(b) > 0 FROM Bid b WHERE b.auction.id = :auctionId AND b.user.id = :userId AND b.bidAmount = :bidAmount")
    boolean existsByAuctionIdAndUserIdAndBidAmount(
            @Param("auctionId") Long auctionId,
            @Param("userId") Long userId,
            @Param("bidAmount") Long bidAmount);
}
