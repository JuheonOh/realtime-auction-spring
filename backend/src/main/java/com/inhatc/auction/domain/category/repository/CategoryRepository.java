package com.inhatc.auction.domain.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}