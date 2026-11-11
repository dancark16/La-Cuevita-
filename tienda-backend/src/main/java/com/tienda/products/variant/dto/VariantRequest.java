package com.tienda.products.variant.dto;

import com.tienda.products.variant.VariantStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantRequest(
        @NotNull
        UUID productId,
        @NotNull
        UUID sizeId,
        @NotNull
        UUID colorId,
        @NotBlank
        String sku,
        String barcode,
        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        BigDecimal price,
        @DecimalMin(value = "0", inclusive = true)
        BigDecimal promoPrice,
        VariantStatus status
        ) {

}
