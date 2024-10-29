package com.inhatc.auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inhatc.auction.domain.Bid;

public interface BidRepository extends JpaRepository<Bid, Long> {
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.auction.id = :auctionId")
    Optional<Long> findBidCountByAuctionId(@Param("auctionId") Long auctionId);

    @Query("SELECT b.user.id FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.bidAmount DESC LIMIT 1")
    Optional<Long> findCurrentHighestBidUserId(@Param("auctionId") Long auctionId);

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.bidAmount DESC")
    List<Bid> findAllByAuctionId(@Param("auctionId") Long auctionId);
}
