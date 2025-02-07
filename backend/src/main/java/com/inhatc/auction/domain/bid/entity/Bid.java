package com.inhatc.auction.domain.bid.entity;

import java.time.LocalDateTime;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.global.entity.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bid")
@Getter
@NoArgsConstructor
public class Bid extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long bidAmount;

    private LocalDateTime bidTime;

    @Builder
    public Bid(Auction auction, User user, Long bidAmount, LocalDateTime bidTime) {
        this.auction = auction;
        this.user = user;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }
}
