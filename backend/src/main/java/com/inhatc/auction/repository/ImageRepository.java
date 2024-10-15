package com.inhatc.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
