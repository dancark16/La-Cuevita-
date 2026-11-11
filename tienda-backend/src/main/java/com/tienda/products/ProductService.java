package com.tienda.products;

import com.tienda.categories.Category;
import com.tienda.categories.CategoryRepository;
import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.products.dto.ProductRequest;
import com.tienda.products.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(UUID categoryId, String q, Pageable pageable) {
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();
        return productRepository.search(categoryId, normalizedQ, pageable)
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return productMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = getCategoryOrThrow(request.categoryId());

        Product product = Product.builder()
                .category(category)
                .name(request.name())
                .description(request.description())
                .baseStatus(request.baseStatus())
                .build();

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getOrThrow(id);
        Category category = getCategoryOrThrow(request.categoryId());

        product.setCategory(category);
        product.setName(request.name());
        product.setDescription(request.description());
        if (request.baseStatus() != null) {
            product.setBaseStatus(request.baseStatus());
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void softDelete(UUID id) {
        Product product = getOrThrow(id);
        product.setBaseStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    private Category getCategoryOrThrow(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + categoryId));
    }

    private Product getOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }
}
