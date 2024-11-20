package com.inhatc.auction.domain.redisBid.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.entity.Bid;
import com.inhatc.auction.domain.bid.repository.BidRepository;
import com.inhatc.auction.domain.redisBid.entity.RedisBid;
import com.inhatc.auction.domain.redisBid.repository.RedisBidRepository;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BidMigrationService {
    private final BidRepository bidRepository;
    private final RedisBidRepository redisBidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void migrateAllBidsToRedis() {
        log.info("입찰 데이터 마이그레이션 (MariaDB -> Redis)");

        List<Bid> allBids = bidRepository.findAll();
        for (Bid bid : allBids) {
            // Redis에 이미 존재하는지 확인
            boolean exists = redisBidRepository.existsByAuctionIdAndUserIdAndBidAmount(
                    bid.getAuction().getId(),
                    bid.getUser().getId(),
                    bid.getBidAmount());

            if (!exists) {
                RedisBid redisBid = RedisBid.builder()
                        .auctionId(bid.getAuction().getId())
                        .userId(bid.getUser().getId())
                        .bidAmount(bid.getBidAmount())
                        .bidTime(bid.getBidTime())
                        .build();

                redisBidRepository.save(redisBid);
            }
        }
    }

    @Transactional
    public void migrateAllBidsToMariaDB() {
        log.info("입찰 데이터 마이그레이션 (Redis -> MariaDB)");

        List<RedisBid> allBids = redisBidRepository.findAllByOrderByAuctionIdAscBidTimeAsc();
        for (RedisBid redisBid : allBids) {
            // 이미 존재하는 입찰인지 확인
            boolean exists = bidRepository.existsByAuctionIdAndUserIdAndBidAmount(
                    redisBid.getAuctionId(),
                    redisBid.getUserId(),
                    redisBid.getBidAmount());

            if (!exists) {
                Auction auction = auctionRepository.findById(redisBid.getAuctionId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다"));
                User user = userRepository.findById(redisBid.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

                Bid bid = Bid.builder()
                        .auction(auction)
                        .user(user)
                        .bidAmount(redisBid.getBidAmount())
                        .bidTime(redisBid.getBidTime())
                        .build();

                bidRepository.save(bid);
            }
        }
    }
}