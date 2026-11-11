package com.tienda.catalogadmin.dto;

import com.tienda.products.ProductStatus;
import com.tienda.products.variant.VariantStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** Una fila del catálogo de administración: una variante (SKU) con los datos de su producto. */
public record CatalogItemResponse(
        UUID variantId,
        String thumbnailUrl,
        UUID productId,
        String sku,
        String productName,
        UUID categoryId,
        String categoryName,
        String sizeLabel,
        String colorName,
        String colorHex,
        BigDecimal price,
        BigDecimal promoPrice,
        int stock,
        VariantStatus variantStatus,
        ProductStatus productStatus
) {
}
