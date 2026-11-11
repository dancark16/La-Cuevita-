package com.tienda.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
            SELECT p FROM Product p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Product> search(@Param("categoryId") UUID categoryId, @Param("q") String q, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.baseStatus = com.tienda.products.ProductStatus.ACTIVE
            AND EXISTS (
                SELECT 1 FROM ProductVariant v
                WHERE v.product = p AND v.status = com.tienda.products.variant.VariantStatus.ACTIVE)
            AND (:categoryId IS NULL OR p.category.id = :categoryId OR p.category.parent.id = :categoryId)
            AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            AND (:size IS NULL OR EXISTS (
                SELECT 1 FROM ProductVariant vs
                WHERE vs.product = p AND vs.status = com.tienda.products.variant.VariantStatus.ACTIVE
                AND vs.size.label = :size))
            AND (:color IS NULL OR EXISTS (
                SELECT 1 FROM ProductVariant vc
                WHERE vc.product = p AND vc.status = com.tienda.products.variant.VariantStatus.ACTIVE
                AND vc.color.name = :color))
            """)
    Page<Product> searchStorefront(@Param("categoryId") UUID categoryId, @Param("q") String q,
                                   @Param("size") String size, @Param("color") String color, Pageable pageable);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category c
            WHERE (:categoryId IS NULL OR c.id = :categoryId)
            AND (:status IS NULL OR CAST(p.baseStatus AS string) = CAST(:status AS string))
            AND (:q IS NULL
                 OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                 OR EXISTS (SELECT 1 FROM ProductVariant v WHERE v.product = p
                            AND LOWER(v.sku) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:status IS NULL OR CAST(p.baseStatus AS string) = CAST(:status AS string))
            AND (:q IS NULL
                 OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                 OR EXISTS (SELECT 1 FROM ProductVariant v WHERE v.product = p
                            AND LOWER(v.sku) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))
            """)
    Page<Product> searchAdmin(@Param("categoryId") UUID categoryId, @Param("q") String q,
                              @Param("status") String status, Pageable pageable);

    Optional<Product> findFirstByNameIgnoreCase(String name);

    @Query("SELECT p.baseStatus, COUNT(p) FROM Product p GROUP BY p.baseStatus")
    java.util.List<Object[]> countByStatus();

    @Query("""
            SELECT c.id, c.name, COUNT(p)
            FROM Category c LEFT JOIN Product p ON p.category = c
            GROUP BY c.id, c.name
            """)
    java.util.List<Object[]> countByCategory();
}
