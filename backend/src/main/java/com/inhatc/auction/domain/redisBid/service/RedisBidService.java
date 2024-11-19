package com.inhatc.auction.domain.redisBid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.inhatc.auction.domain.redisBid.entity.RedisBid;
import com.inhatc.auction.domain.redisBid.repository.RedisBidRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisBidService {
    private final RedisBidRepository redisBidRepository;

    public RedisBid saveBid(RedisBid bid) {
        log.info("New bid saved for auction {}: {}", bid.getAuctionId(), bid);
        return redisBidRepository.save(bid);
    }

    public List<RedisBid> getBidsByAuctionId(Long auctionId) {
        return redisBidRepository.findByAuctionIdOrderByBidTimeAsc(auctionId);
    }

    public RedisBid getLatestBid(Long auctionId) {
        return redisBidRepository.findFirstByAuctionIdOrderByBidTimeDesc(auctionId)
                .orElse(null);
    }

    public Long getBidCount(Long auctionId) {
        return redisBidRepository.countByAuctionId(auctionId);
    }

    public List<RedisBid> getUserBids(Long userId) {
        return redisBidRepository.findByUserIdOrderByBidTimeDesc(userId);
    }

    public boolean isHighestBidder(Long auctionId, Long userId) {
        return redisBidRepository.findFirstByAuctionIdOrderByBidTimeDesc(auctionId)
                .map(bid -> bid.getUserId().equals(userId))
                .orElse(false);
    }
}
