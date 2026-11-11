package com.tienda.catalogadmin.dto;

import com.tienda.products.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Una fila del catálogo de administración: un producto con todas sus variantes (colores y tallas). */
public record ProductRowResponse(
        UUID productId,
        String name,
        UUID categoryId,
        String categoryName,
        String thumbnailUrl,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean onPromo,
        int totalStock,
        List<Variant> variants
) {
    public record Variant(
            UUID variantId,
            String sku,
            String sizeLabel,
            String colorName,
            String colorHex,
            BigDecimal price,
            BigDecimal promoPrice,
            int stock
    ) {
    }
}
