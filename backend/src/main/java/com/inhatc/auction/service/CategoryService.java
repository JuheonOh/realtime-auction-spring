package com.inhatc.auction.service;

import com.inhatc.auction.domain.Category;
import com.inhatc.auction.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public void insertInitialCategories() {

        // 이미 데이터가 존재하면 초기화하지 않음
        if (categoryRepository.count() > 0) {
            return;
        }

        // 루트 카테고리 생성
        Category electronics = createCategory("Electronics", null);
        Category clothing = createCategory("Clothing", null);

        // 전자제품 하위 카테고리
        Category smartphones = createCategory("Smartphones", electronics.getId());
        Category laptops = createCategory("Laptops", electronics.getId());

        // 의류 하위 카테고리
        Category mens = createCategory("Men's", clothing.getId());
        Category womens = createCategory("Women's", clothing.getId());

        // 스마트폰 하위 카테고리
        createCategory("Android", smartphones.getId());
        createCategory("iOS", smartphones.getId());
    }

    @Transactional
    public Category createCategory(String name, Long parentId) {
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
        }

        if (categoryRepository.existsByNameAndParent(name, parent)) {
            throw new RuntimeException("Category with this name already exists under the given parent");
        }

        Category category = new Category();
        category.setName(name);
        category.setParent(parent);
        return categoryRepository.save(category);
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getSubcategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Category> getAllSubcategories(Long categoryId) {
        return categoryRepository.findAllSubcategories(categoryId);
    }

    @Transactional
    public Category updateCategory(Long id, String newName, Long newParentId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (newName != null && !newName.isEmpty()) {
            category.setName(newName);
        }

        if (newParentId != null) {
            Category newParent = categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("New parent category not found"));
            category.setParent(newParent);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getChildren().isEmpty()) {
            throw new RuntimeException("Cannot delete category with subcategories");
        }

        categoryRepository.delete(category);
    }
}