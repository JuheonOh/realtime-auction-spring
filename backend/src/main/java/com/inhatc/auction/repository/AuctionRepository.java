package com.inhatc.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.Auction;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
