package com.inhatc.auction;

import com.inhatc.auction.domain.Category;
import com.inhatc.auction.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void shouldInsertTestData() {
        // 루트 카테고리 생성
        Category electronics = createCategory("Electronics", null);
        Category clothing = createCategory("Clothing", null);

        // 전자제품 하위 카테고리
        Category smartphones = createCategory("Smartphones", electronics);
        Category laptops = createCategory("Laptops", electronics);

        // 의류 하위 카테고리
        Category mens = createCategory("Men's", clothing);
        Category womens = createCategory("Women's", clothing);

        // 스마트폰 하위 카테고리
        createCategory("Android", smartphones);
        createCategory("iOS", smartphones);

        // 데이터 검증
        List<Category> rootCategories = categoryRepository.findByParentIsNull();
        assertThat(rootCategories).hasSize(2);

        List<Category> electronicsSubcategories = categoryRepository.findByParentId(electronics.getId());
        assertThat(electronicsSubcategories).hasSize(2);

        List<Category> smartphonesSubcategories = categoryRepository.findByParentId(smartphones.getId());
        assertThat(smartphonesSubcategories).hasSize(2);
    }

    private Category createCategory(String name, Category parent) {
        Category category = new Category();
        category.setName(name);
        category.setParent(parent);
        return categoryRepository.save(category);
    }
}