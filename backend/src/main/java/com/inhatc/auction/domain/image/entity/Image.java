package com.inhatc.auction.domain.image.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inhatc.auction.domain.auction.entity.Auction;
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
@Table(name = "image")
@Getter
@NoArgsConstructor
public class Image extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    private String fileName;
    private String filePath;
    private String fileType;
    private long fileSize;

    @Builder
    public Image(Auction auction, String fileName, String filePath, String fileType, long fileSize) {
        this.auction = auction;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }
}
