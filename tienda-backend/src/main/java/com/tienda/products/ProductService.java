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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;


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

    /**
     * Elimina el producto por completo si nunca se vendió. Si ya tiene pedidos, solo se
     * desactiva (se oculta) para no romper el historial. Devuelve true si se borró.
     */
    @Transactional
    public boolean deletePermanently(UUID id) {
        Product product = getOrThrow(id);
        long sold = entityManager
                .createQuery("SELECT COUNT(i) FROM OrderItem i WHERE i.variant.product.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        if (sold > 0) {
            product.setBaseStatus(ProductStatus.INACTIVE);
            productRepository.save(product);
            return false;
        }
        entityManager.createQuery(
                "DELETE FROM InventoryMovement m WHERE m.inventory.id IN "
                        + "(SELECT n.id FROM Inventory n WHERE n.variant.id IN (SELECT v.id FROM ProductVariant v WHERE v.product.id = :id))")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM Inventory n WHERE n.variant.id IN (SELECT v.id FROM ProductVariant v WHERE v.product.id = :id)")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM ProductImage im WHERE im.variant.id IN (SELECT v.id FROM ProductVariant v WHERE v.product.id = :id)")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM ProductVariant v WHERE v.product.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        productRepository.deleteById(id);
        return true;
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
