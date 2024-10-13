package com.inhatc.auction.repository;

import com.inhatc.auction.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIsNull();
    List<Category> findByParentId(Long parentId);
    Optional<Category> findByName(String name);

    @Query(nativeQuery = true, value =
            "WITH RECURSIVE subcategories AS (" +
                    "  SELECT id, name, parent_id FROM category WHERE id = :categoryId " +
                    "  UNION ALL " +
                    "  SELECT c.id, c.name, c.parent_id FROM category c " +
                    "  INNER JOIN subcategories s ON c.parent_id = s.id" +
                    ") " +
                    "SELECT * FROM subcategories")
    List<Category> findAllSubcategories(@Param("categoryId") Long categoryId);

    boolean existsByNameAndParent(String name, Category parent);
}