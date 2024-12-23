package com.inhatc.auction.domain.auction.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.inhatc.auction.domain.bid.entity.Bid;
import com.inhatc.auction.domain.category.entity.Category;
import com.inhatc.auction.domain.image.entity.Image;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.global.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auction")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auction extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long startPrice;
    private Long buyNowPrice;
    private Long currentPrice;
    private Long successfulPrice;

    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<Image> images;

    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<Bid> bids;

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public void updateAuctionEndTime(LocalDateTime auctionEndTime) {
        this.auctionEndTime = auctionEndTime;
    }

    public void setSuccessfulPrice(Long successfulPrice) {
        this.successfulPrice = successfulPrice;
    }

    public void updateCurrentPrice(Long currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void updateStatus(AuctionStatus status) {
        this.status = status;
    }

    public void extendEndTime(Duration downtime) {
        this.auctionEndTime = this.auctionEndTime.plus(downtime);
    }

    public Long getAuctionLeftTime() {
        return Math.max(0, Duration.between(LocalDateTime.now(), this.auctionEndTime).toSeconds());
    }
}