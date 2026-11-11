package com.tienda.products.variant.dto;

import com.tienda.products.variant.VariantStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID sizeId,
        String sizeLabel,
        UUID colorId,
        String colorName,
        String colorHex,
        String sku,
        String barcode,
        BigDecimal price,
        BigDecimal promoPrice,
        VariantStatus status,
        List<ImageResponse> images,
        Instant createdAt,
        Instant updatedAt
        ) {

}
