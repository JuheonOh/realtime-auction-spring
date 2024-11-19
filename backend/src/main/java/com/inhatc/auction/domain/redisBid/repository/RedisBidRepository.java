package com.inhatc.auction.domain.redisBid.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.inhatc.auction.domain.redisBid.entity.RedisBid;

@Repository
public interface RedisBidRepository extends CrudRepository<RedisBid, String> {
    // 경매별 모든 입찰 내역 조회 (시간 순)
    List<RedisBid> findByAuctionIdOrderByBidTimeAsc(Long auctionId);

    // 경매별 모든 입찰 내역 조회 (시간 역순)
    List<RedisBid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    // 경매의 최신 입찰 조회
    Optional<RedisBid> findFirstByAuctionIdOrderByBidTimeDesc(Long auctionId);

    // 사용자별 입찰 내역 조회
    List<RedisBid> findByUserIdOrderByBidTimeDesc(Long userId);

    // 경매별 입찰 수 조회
    Long countByAuctionId(Long auctionId);
}
