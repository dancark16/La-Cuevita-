package com.tienda.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID variantId,
        String sku,
        String productName,
        String sizeLabel,
        String colorName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}