package com.inhatc.auction.domain;

import com.inhatc.auction.common.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long startPrice;
    private Long buyNowPrice;
    private Long successfulPrice;

    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Image> images;

    @OneToMany(mappedBy = "auction")
    @ToString.Exclude
    private List<Bid> bids;


    @Builder
    public Auction(User user, Category category, String title, String description, Long startPrice, Long buyNowPrice,
            LocalDateTime auctionStartTime, LocalDateTime auctionEndTime, AuctionStatus status, LocalDateTime createdAt,
            LocalDateTime updatedAt, List<Image> images) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.description = description;
        this.startPrice = startPrice;
        this.buyNowPrice = buyNowPrice;
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.images = images;
    }
}