package com.inhatc.auction.domain.image.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.image.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByAuctionId(Long auctionId);
}
