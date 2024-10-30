package com.inhatc.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByAuctionId(Long auctionId);
}
