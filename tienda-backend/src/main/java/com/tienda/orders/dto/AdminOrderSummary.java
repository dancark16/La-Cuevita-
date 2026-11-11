package com.tienda.orders.dto;

import com.tienda.orders.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderSummary(
        UUID id,
        Long orderNumber,
        OrderStatus status,
        String customerName,
        String customerPhone,
        BigDecimal total,
        int itemCount,
        boolean hasReceipt,
        Instant receiptUploadedAt,
        Instant createdAt
) {
}
