package com.inhatc.auction.repository;

import com.inhatc.auction.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}