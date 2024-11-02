package com.inhatc.auction.domain.transaction.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.global.constant.TransactionStatus;
import com.inhatc.auction.global.entity.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor
public class Transaction extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Builder
    public Transaction(Auction auction, User seller, User buyer, Long finalPrice, TransactionStatus status) {
        this.auction = auction;
        this.seller = seller;
        this.buyer = buyer;
        this.finalPrice = finalPrice;
        this.status = status;
    }
}