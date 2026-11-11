package com.tienda.products.variant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductId(UUID productId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.size
            JOIN FETCH v.color
            LEFT JOIN FETCH v.images
            WHERE v.product.id IN :productIds
            AND v.status = com.tienda.products.variant.VariantStatus.ACTIVE
            """)
    List<ProductVariant> findActiveWithDetailsByProductIds(@Param("productIds") Collection<UUID> productIds);

    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.size
            JOIN FETCH v.color
            LEFT JOIN FETCH v.images
            WHERE v.product.id IN :productIds
            AND v.status <> com.tienda.products.variant.VariantStatus.DISCONTINUED
            """)
    List<ProductVariant> findAvailableWithDetailsByProductIds(@Param("productIds") Collection<UUID> productIds);

    Optional<ProductVariant> findBySku(String sku);

    @Query("""
            SELECT DISTINCT v.size.label FROM ProductVariant v
            WHERE v.status = com.tienda.products.variant.VariantStatus.ACTIVE
            AND v.product.baseStatus = com.tienda.products.ProductStatus.ACTIVE
            """)
    List<String> findActiveSizeLabels();

    @Query("""
            SELECT DISTINCT v.color.name, v.color.hex FROM ProductVariant v
            WHERE v.status = com.tienda.products.variant.VariantStatus.ACTIVE
            AND v.product.baseStatus = com.tienda.products.ProductStatus.ACTIVE
            """)
    List<Object[]> findActiveColors();

    @Query(value = """
            SELECT v FROM ProductVariant v
            JOIN FETCH v.product p
            JOIN FETCH p.category c
            JOIN FETCH v.size
            JOIN FETCH v.color
            WHERE (:categoryId IS NULL OR c.id = :categoryId)
            AND (:status IS NULL OR CAST(p.baseStatus AS string) = CAST(:status AS string))
            AND (:q IS NULL
                 OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                 OR LOWER(v.sku) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(v) FROM ProductVariant v
            JOIN v.product p
            JOIN p.category c
            WHERE (:categoryId IS NULL OR c.id = :categoryId)
            AND (:status IS NULL OR CAST(p.baseStatus AS string) = CAST(:status AS string))
            AND (:q IS NULL
                 OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                 OR LOWER(v.sku) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<ProductVariant> searchAdmin(@Param("categoryId") UUID categoryId,
                                     @Param("q") String q,
                                     @Param("status") String status,
                                     Pageable pageable);
}
