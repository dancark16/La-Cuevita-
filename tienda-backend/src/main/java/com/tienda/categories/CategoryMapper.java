package com.tienda.categories;

import com.tienda.categories.dto.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getImageUrl(),
                category.isActive(),
                category.getCreatedAt());
    }
}
