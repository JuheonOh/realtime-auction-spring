package com.inhatc.auction.domain.migration.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.entity.Bid;
import com.inhatc.auction.domain.bid.entity.RedisBid;
import com.inhatc.auction.domain.bid.repository.BidRepository;
import com.inhatc.auction.domain.bid.repository.RedisBidRepository;
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

        List<RedisBid> allBids = redisBidRepository.findAllByOrderByBidTimeAsc();
        for (RedisBid redisBid : allBids) {

            if (redisBid == null) {
                log.warn("Redis에서 null bid 발견");
                continue;
            }

            // 이미 존재하는 입찰인지 확인
            boolean exists = bidRepository.existsByAuctionIdAndUserIdAndBidAmount(
                    redisBid.getAuctionId(),
                    redisBid.getUserId(),
                    redisBid.getBidAmount());

            if (!exists) {
                Optional<Auction> auction = auctionRepository.findById(redisBid.getAuctionId());
                Optional<User> user = userRepository.findById(redisBid.getUserId());

                if (auction.isPresent() && user.isPresent()) {
                    Bid bid = Bid.builder()
                            .auction(auction.get())
                            .user(user.get())
                            .bidAmount(redisBid.getBidAmount())
                            .bidTime(redisBid.getBidTime())
                            .build();

                    bidRepository.save(bid);
                }
            }
        }
    }
}