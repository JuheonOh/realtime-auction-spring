package com.inhatc.auction.domain.category.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inhatc.auction.domain.category.entity.Category;
import com.inhatc.auction.domain.category.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> getCategoryList() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category createCategory(String name) {
        Category category = Category.builder()
                .name(name)
                .build();

        return categoryRepository.save(category);
    }
}