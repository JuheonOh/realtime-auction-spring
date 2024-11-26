package com.inhatc.auction.domain.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.favorite.entity.Favorite;
import com.inhatc.auction.domain.user.entity.User;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 관심한 경매 개수
    Long countByAuctionId(Long auctionId);

    // 경매에 대한 관심 목록
    List<Favorite> findByAuction(Auction auction);

    // 관심한 경매인지 확인
    boolean existsByUserAndAuction(User user, Auction auction);

    // 관심한 경매 삭제
    void deleteByUserAndAuction(User user, Auction auction);
}
