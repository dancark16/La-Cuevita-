package com.tienda.categories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsBySlugAndIdNot(String slug, UUID id);
    boolean existsBySlug(String slug);
    Optional<Category> findBySlug(String slug);
}