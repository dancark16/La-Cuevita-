package com.tienda.categories;

import com.tienda.categories.dto.CategoryRequest;
import com.tienda.categories.dto.CategoryResponse;
import com.tienda.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return categoryMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Ya existe una categoría con el slug: " + request.slug());
        }

        Category category = Category.builder()
                .name(request.name())
                .slug(request.slug())
                .parent(resolveParent(request.parentId()))
                .imageUrl(normalizeImageUrl(request.imageUrl()))
                .active(request.active() == null || request.active())
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = getOrThrow(id);

        if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new IllegalArgumentException("Ya existe otra categoría con el slug: " + request.slug());
        }

        category.setName(request.name());
        category.setSlug(request.slug());
        category.setParent(resolveParent(request.parentId()));
        category.setImageUrl(normalizeImageUrl(request.imageUrl()));
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = getOrThrow(id);
        categoryRepository.delete(category);
    }

    private Category resolveParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría padre no encontrada: " + parentId));
    }

    private String normalizeImageUrl(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
    }

    private Category getOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }
}
